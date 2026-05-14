package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.ports.provided.StartStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
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
    private static final String STRAVA_READ_SCOPE = "read";

    private final StravaAuthorizationStateRepository authorizationStateRepository;
    private final StravaOAuthProvider oAuthProvider;
    private final TimeProvider timeProvider;
    private final StravaAuthorizationStateGenerator stateGenerator;

    @Override
    public StravaAuthorizationUrlResult execute(StartStravaAccountLinkingCommand command) {
        Instant expiresAt = timeProvider.now().plus(STATE_DURATION);
        String state = stateGenerator.generate();
        StravaAuthorizationState authorizationState = StravaAuthorizationState.pending(state, command.userEmail(),
                expiresAt);

        authorizationStateRepository.save(authorizationState);

        String authorizationUrl = oAuthProvider.buildAuthorizationUrl(
                new StravaAuthorizationRequest(state, null, STRAVA_READ_SCOPE)
        );
        log.info("Strava account linking started for userEmail={}", command.userEmail());

        return new StravaAuthorizationUrlResult(authorizationUrl);
    }
}
