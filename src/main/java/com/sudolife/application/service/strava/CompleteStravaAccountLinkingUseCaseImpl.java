package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
import com.sudolife.application.service.strava.exception.InsufficientStravaScopeException;
import com.sudolife.application.service.strava.exception.InvalidStravaAuthorizationStateException;
import com.sudolife.application.service.strava.exception.StravaAccountLinkingException;
import com.sudolife.application.service.strava.exception.StravaAuthorizationDeniedException;
import com.sudolife.application.service.strava.exception.StravaAuthorizationFailureException;
import com.sudolife.application.service.strava.ports.provided.CompleteStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteStravaAccountLinkingUseCaseImpl implements CompleteStravaAccountLinkingUseCase {

    private static final String STRAVA_ACTIVITY_READ_SCOPE = "activity:read";

    private final StravaAuthorizationStateRepository authorizationStateRepository;
    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaOAuthProvider oAuthProvider;
    private final TimeProvider timeProvider;
    private final TransactionTemplate transactionTemplate;

    @Override
    public StravaCallbackResult execute(CompleteStravaAccountLinkingCommand command) {
        try {
            return completeLinking(command);
        } catch (StravaAccountLinkingException exception) {
            return failed(exception.getFailureCode());
        }
    }

    private StravaCallbackResult completeLinking(CompleteStravaAccountLinkingCommand command) {
        Instant now = timeProvider.now();
        StravaAuthorizationState authorizationState = consumeValidState(command.state(), now);

        if (hasText(command.error())) {
            log.warn("Strava account linking denied for userEmail={}", authorizationState.getUserEmail());
            throw new StravaAuthorizationDeniedException();
        }

        StravaTokenAuthorization tokenAuthorization = exchangeAuthorizationCode(command.code());
        validateScope(command.scope());
        validateAthleteOwnership(authorizationState.getUserEmail(), tokenAuthorization.athleteId());
        saveActiveLink(authorizationState.getUserEmail(), tokenAuthorization, scopeValue(command.scope()), now);
        log.info("Strava account linking completed for userEmail={} athleteId={}", authorizationState.getUserEmail(),
                tokenAuthorization.athleteId());

        return new StravaCallbackResult(true, null);
    }

    private StravaAuthorizationState consumeValidState(String state, Instant now) {
        if (!hasText(state)) {
            log.warn("Strava account linking failed failureCode=INVALID_STATE");
            throw new InvalidStravaAuthorizationStateException();
        }

        return authorizationStateRepository.consumePending(state, now, now)
                .orElseThrow(() -> {
                    log.warn("Strava account linking failed failureCode=INVALID_STATE");

                    return new InvalidStravaAuthorizationStateException();
                });
    }

    private StravaTokenAuthorization exchangeAuthorizationCode(String code) {
        if (!hasText(code)) {
            log.warn("Strava account linking failed failureCode=TOKEN_EXCHANGE_FAILED");
            throw new StravaAuthorizationFailureException();
        }

        try {
            return oAuthProvider.exchangeAuthorizationCode(code);
        } catch (StravaAccountLinkingException exception) {
            log.warn("Strava account linking failed failureCode={}", exception.getFailureCode());
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Strava account linking failed failureCode=TOKEN_EXCHANGE_FAILED");
            throw new StravaAuthorizationFailureException();
        }
    }

    private void validateScope(String scope) {
        boolean hasActivityReadScope = Arrays.stream(scopeValue(scope).split("[,\\s]+"))
                .anyMatch(STRAVA_ACTIVITY_READ_SCOPE::equals);

        if (!hasActivityReadScope) {
            log.warn("Strava account linking failed failureCode=INSUFFICIENT_SCOPE");
            throw new InsufficientStravaScopeException();
        }
    }

    private String scopeValue(String scope) {
        if (scope == null) {
            return "";
        }

        return scope.trim();
    }

    private void validateAthleteOwnership(String userEmail, Long athleteId) {
        Optional<StravaAccountLink> activeAthleteLink = accountLinkRepository.findActiveByAthleteId(athleteId);

        if (activeAthleteLink.isPresent() && !activeAthleteLink.get().getUserEmail().equals(userEmail)) {
            log.warn("Strava duplicate athlete rejected for athleteId={}", athleteId);
            throw new DuplicateStravaAthleteOwnershipException();
        }
    }

    private void saveActiveLink(String userEmail, StravaTokenAuthorization tokenAuthorization, String grantedScopes,
                                Instant linkedAt) {
        Optional<StravaAccountLink> activeUserLink = accountLinkRepository.findActiveByUserEmail(userEmail);
        Long linkId = activeUserLink.map(StravaAccountLink::getId).orElse(null);
        StravaAccountLink link = StravaAccountLink.active(linkId, userEmail, tokenAuthorization.athleteId(),
                tokenAuthorization.accessToken(), tokenAuthorization.refreshToken(), tokenAuthorization.expiresAt(),
                grantedScopes, linkedAt);

        transactionTemplate.executeWithoutResult(status -> accountLinkRepository.save(link));
    }

    private StravaCallbackResult failed(String failureCode) {
        return new StravaCallbackResult(false, failureCode);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
