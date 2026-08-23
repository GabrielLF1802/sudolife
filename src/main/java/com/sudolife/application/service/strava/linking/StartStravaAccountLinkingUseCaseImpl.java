package com.sudolife.application.service.strava.linking;

import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.model.strava.StravaDataConsentPurpose;
import com.sudolife.application.model.strava.StravaDataConsentRecord;
import com.sudolife.application.model.strava.StravaDataConsentVersions;
import com.sudolife.application.service.strava.authorization.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.authorization.StravaAuthorizationStateGenerator;
import com.sudolife.application.service.strava.exception.MissingStravaDataConsentException;
import com.sudolife.application.service.strava.ports.provided.StartStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
import com.sudolife.application.service.strava.ports.required.StravaDataConsentRepository;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartStravaAccountLinkingUseCaseImpl implements StartStravaAccountLinkingUseCase {

    private static final Duration STATE_DURATION = Duration.ofMinutes(10);
    private static final String STRAVA_LINKING_SCOPE = "read,activity:read,profile:read_all";

    private final StravaAuthorizationStateRepository authorizationStateRepository;
    private final StravaDataConsentRepository consentRepository;
    private final StravaOAuthProvider oAuthProvider;
    private final TimeProvider timeProvider;
    private final StravaAuthorizationStateGenerator stateGenerator;

    @Override
    public StravaAuthorizationUrlResult execute(StartStravaAccountLinkingCommand command) {
        Instant now = timeProvider.now();
        ensureConsent(command, now);
        Instant expiresAt = now.plus(STATE_DURATION);
        String state = stateGenerator.generate();
        StravaAuthorizationState authorizationState = StravaAuthorizationState.pending(state, command.userEmail(),
                expiresAt);

        authorizationStateRepository.save(authorizationState);

        String authorizationUrl = oAuthProvider.buildAuthorizationUrl(
                new StravaAuthorizationRequest(state, null, STRAVA_LINKING_SCOPE)
        );
        log.info("Strava account linking started for userEmail={}", command.userEmail());

        return new StravaAuthorizationUrlResult(authorizationUrl);
    }

    private void ensureConsent(StartStravaAccountLinkingCommand command, Instant now) {
        if (hasCurrentConsent(command.userEmail())) {
            return;
        }

        if (!command.acceptedStravaDataConsent()) {
            throw new MissingStravaDataConsentException();
        }

        consentRepository.save(StravaDataConsentRecord.accepted(command.userEmail(), command.language(), now));
    }

    private boolean hasCurrentConsent(String userEmail) {
        return consentRepository.existsByUserEmailAndPurposeAndConsentVersion(userEmail,
                StravaDataConsentPurpose.STRAVA_DATA_IMPORT_AND_COACHING, StravaDataConsentVersions.CURRENT);
    }
}
