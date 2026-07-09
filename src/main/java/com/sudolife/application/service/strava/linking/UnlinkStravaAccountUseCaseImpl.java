package com.sudolife.application.service.strava.linking;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.ports.provided.UnlinkStravaAccountUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaImportedDataRepository;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnlinkStravaAccountUseCaseImpl implements UnlinkStravaAccountUseCase {

    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaImportedDataRepository importedDataRepository;
    private final StravaOAuthProvider oAuthProvider;
    private final TimeProvider timeProvider;
    private final TransactionTemplate transactionTemplate;

    @Override
    public UnlinkStravaAccountResult execute(UnlinkStravaAccountCommand command) {
        return accountLinkRepository.findActiveByUserEmail(command.userEmail())
                .map(this::unlink)
                .orElseGet(this::alreadyUnlinked);
    }

    private UnlinkStravaAccountResult unlink(StravaAccountLink accountLink) {
        Instant now = timeProvider.now();
        StravaAuthorizationSnapshot authorizationSnapshot = StravaAuthorizationSnapshot.from(accountLink);
        accountLink.deactivate(now);
        transactionTemplate.executeWithoutResult(status -> {
            importedDataRepository.deleteByAccountLinkId(accountLink.getId());
            accountLinkRepository.save(accountLink);
        });
        deauthorizeWhenPossible(accountLink, authorizationSnapshot, now);
        log.info("Strava account unlinked for userEmail={} athleteId={}", accountLink.getUserEmail(),
                accountLink.getAthleteId());

        return new UnlinkStravaAccountResult(true);
    }

    private void deauthorizeWhenPossible(StravaAccountLink accountLink, StravaAuthorizationSnapshot authorizationSnapshot, Instant now) {
        String accessToken = usableAccessToken(authorizationSnapshot, now);

        if (!hasText(accessToken)) {
            accessToken = refreshAccessToken(accountLink, authorizationSnapshot);
        }

        if (hasText(accessToken)) {
            deauthorize(accountLink, accessToken);
        }
    }

    private String usableAccessToken(StravaAuthorizationSnapshot authorizationSnapshot, Instant now) {
        if (!hasText(authorizationSnapshot.accessToken()) || authorizationSnapshot.expiresAt() == null) {
            return null;
        }

        if (authorizationSnapshot.expiresAt().isAfter(now)) {
            return authorizationSnapshot.accessToken();
        }

        return null;
    }

    private String refreshAccessToken(StravaAccountLink accountLink, StravaAuthorizationSnapshot authorizationSnapshot) {
        if (!hasText(authorizationSnapshot.refreshToken())) {
            return null;
        }

        try {
            return oAuthProvider.refresh(authorizationSnapshot.refreshToken()).accessToken();
        } catch (RuntimeException exception) {
            log.warn("Strava token refresh failed during unlink for userEmail={} athleteId={}",
                    accountLink.getUserEmail(), accountLink.getAthleteId());

            return null;
        }
    }

    private void deauthorize(StravaAccountLink accountLink, String accessToken) {
        try {
            oAuthProvider.deauthorize(accessToken);
        } catch (RuntimeException exception) {
            log.warn("Strava deauthorization failed during unlink for userEmail={} athleteId={}",
                    accountLink.getUserEmail(), accountLink.getAthleteId());
        }
    }

    private UnlinkStravaAccountResult alreadyUnlinked() {
        return new UnlinkStravaAccountResult(true);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record StravaAuthorizationSnapshot(String accessToken, String refreshToken, Instant expiresAt) {

        private static StravaAuthorizationSnapshot from(StravaAccountLink accountLink) {
            return new StravaAuthorizationSnapshot(accountLink.getAccessToken(), accountLink.getRefreshToken(),
                    accountLink.getExpiresAt());
        }
    }
}
