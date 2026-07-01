package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.exception.StravaActivityUnauthorizedException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.exception.StravaAuthorizationFailureException;
import com.sudolife.application.service.strava.exception.StravaReconnectRequiredException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ROTATED_ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ROTATED_REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StravaAccessTokenServiceUnitTest {

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaOAuthProvider oAuthProvider;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private StravaAccessTokenService service;

    @Test
    void executeWithValidToken_when_token_near_expiry_refreshes_and_persists_rotated_credentials() {
        ReflectionTestUtils.setField(service, "refreshBeforeExpiry", Duration.ofHours(7));
        when(timeProvider.now()).thenReturn(NOW);
        when(oAuthProvider.refresh(REFRESH_TOKEN)).thenReturn(rotatedAuthorization());
        when(accountLinkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String token = service.executeWithValidToken(activeStravaAccountLink(), StravaAccountLink::getAccessToken);

        StravaAccountLink savedLink = capturedLink();
        assertThat(token).isEqualTo(ROTATED_ACCESS_TOKEN);
        assertThat(savedLink.getAccessToken()).isEqualTo(ROTATED_ACCESS_TOKEN);
        assertThat(savedLink.getRefreshToken()).isEqualTo(ROTATED_REFRESH_TOKEN);
        assertThat(savedLink.getExpiresAt()).isEqualTo(EXPIRES_AT.plus(Duration.ofHours(1)));
        assertThat(savedLink.isReconnectRequired()).isFalse();
    }

    @Test
    void executeWithValidToken_when_activity_call_returns_unauthorized_refreshes_and_retries_once() {
        ReflectionTestUtils.setField(service, "refreshBeforeExpiry", Duration.ofMinutes(5));
        when(timeProvider.now()).thenReturn(NOW);
        when(oAuthProvider.refresh(REFRESH_TOKEN)).thenReturn(rotatedAuthorization());
        when(accountLinkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String token = service.executeWithValidToken(activeStravaAccountLink(), new UnauthorizedOnceCall());

        assertThat(token).isEqualTo(ROTATED_ACCESS_TOKEN);
    }

    @Test
    void executeWithValidToken_when_retry_still_returns_unauthorized_fails_operation() {
        ReflectionTestUtils.setField(service, "refreshBeforeExpiry", Duration.ofMinutes(5));
        when(timeProvider.now()).thenReturn(NOW);
        when(oAuthProvider.refresh(REFRESH_TOKEN)).thenReturn(rotatedAuthorization());
        when(accountLinkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.executeWithValidToken(activeStravaAccountLink(), link -> {
            throw new StravaActivityUnauthorizedException();
        })).isInstanceOf(StravaActivityUnavailableException.class);
    }

    @Test
    void executeWithValidToken_when_refresh_permanently_fails_marks_reconnect_required() {
        ReflectionTestUtils.setField(service, "refreshBeforeExpiry", Duration.ofHours(7));
        when(timeProvider.now()).thenReturn(NOW);
        when(oAuthProvider.refresh(REFRESH_TOKEN)).thenThrow(new StravaAuthorizationFailureException());

        assertThatThrownBy(() -> service.executeWithValidToken(activeStravaAccountLink(),
                StravaAccountLink::getAccessToken))
                .isInstanceOf(StravaReconnectRequiredException.class);

        assertThat(capturedLink().isReconnectRequired()).isTrue();
    }

    private StravaTokenAuthorization rotatedAuthorization() {
        return new StravaTokenAuthorization(ATHLETE_ID, ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN,
                EXPIRES_AT.plus(Duration.ofHours(1)), SCOPE);
    }

    private StravaAccountLink capturedLink() {
        ArgumentCaptor<StravaAccountLink> captor = ArgumentCaptor.forClass(StravaAccountLink.class);
        verify(accountLinkRepository).save(captor.capture());

        return captor.getValue();
    }

    private static class UnauthorizedOnceCall implements java.util.function.Function<StravaAccountLink, String> {

        private boolean unauthorized = true;

        @Override
        public String apply(StravaAccountLink accountLink) {
            if (unauthorized) {
                unauthorized = false;
                throw new StravaActivityUnauthorizedException();
            }

            return accountLink.getAccessToken();
        }
    }
}
