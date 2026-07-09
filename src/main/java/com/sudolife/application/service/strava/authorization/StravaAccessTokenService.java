package com.sudolife.application.service.strava.authorization;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.exception.StravaActivityUnauthorizedException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.exception.StravaAuthorizationFailureException;
import com.sudolife.application.service.strava.exception.StravaReconnectRequiredException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class StravaAccessTokenService {

    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaOAuthProvider oAuthProvider;
    private final TimeProvider timeProvider;
    @Value("${strava.token-refresh.refresh-before-expiry:PT5M}")
    private Duration refreshBeforeExpiry;

    public <T> T executeWithValidToken(StravaAccountLink accountLink, Function<StravaAccountLink, T> activityCall) {
        StravaAccountLink refreshedLink = refreshIfNecessary(accountLink);

        try {
            return activityCall.apply(refreshedLink);
        } catch (StravaActivityUnauthorizedException exception) {
            StravaAccountLink retryLink = refresh(refreshedLink, true);

            try {
                return activityCall.apply(retryLink);
            } catch (StravaActivityUnauthorizedException retryException) {
                throw new StravaActivityUnavailableException(retryException);
            }
        }
    }

    public StravaAccountLink refreshIfNecessary(StravaAccountLink accountLink) {
        if (accountLink.isReconnectRequired()) {
            throw new StravaReconnectRequiredException();
        }

        if (!accountLink.expiresWithin(timeProvider.now(), refreshBeforeExpiry)) {
            return accountLink;
        }

        return refresh(accountLink, false);
    }

    private StravaAccountLink refresh(StravaAccountLink accountLink, boolean forced) {
        try {
            StravaTokenAuthorization authorization = oAuthProvider.refresh(accountLink.getRefreshToken());
            accountLink.rotateAuthorization(authorization.accessToken(), authorization.refreshToken(),
                    authorization.expiresAt(), authorization.scope());
            StravaAccountLink savedLink = accountLinkRepository.save(accountLink);
            log.info("Strava token refreshed userEmail={} accountLinkId={} forced={}", savedLink.getUserEmail(),
                    savedLink.getId(), forced);

            return savedLink;
        } catch (StravaAuthorizationFailureException exception) {
            accountLink.markReconnectRequired();
            accountLinkRepository.save(accountLink);
            log.warn("Strava account reconnect required userEmail={} accountLinkId={} forced={}",
                    accountLink.getUserEmail(), accountLink.getId(), forced);
            throw new StravaReconnectRequiredException();
        }
    }
}
