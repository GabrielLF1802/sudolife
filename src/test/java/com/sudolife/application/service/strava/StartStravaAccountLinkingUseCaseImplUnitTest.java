package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;

import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.startStravaAccountLinkingCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartStravaAccountLinkingUseCaseImplUnitTest {

    private static final String AUTHORIZATION_URL = "https://www.strava.com/oauth/authorize?state=state-token";
    private static final Instant EXPECTED_EXPIRATION = Instant.parse("2026-05-11T12:10:00Z");

    @Mock
    private StravaAuthorizationStateRepository authorizationStateRepository;

    @Mock
    private StravaOAuthProvider oAuthProvider;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private StravaAuthorizationStateGenerator stateGenerator;

    @InjectMocks
    private StartStravaAccountLinkingUseCaseImpl useCase;

    @Test
    void execute_persists_state_with_user_email_and_expiration_from_time_provider() {
        stubSuccessfulStart();

        useCase.execute(startStravaAccountLinkingCommand());

        StravaAuthorizationState savedState = capturedSavedState();
        assertThat(savedState.getState()).isEqualTo(STATE);
        assertThat(savedState.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(savedState.getExpiresAt()).isEqualTo(EXPECTED_EXPIRATION);
    }

    @Test
    void execute_requests_authorization_url_with_activity_read_scope() {
        stubSuccessfulStart();

        useCase.execute(startStravaAccountLinkingCommand());

        StravaAuthorizationRequest request = capturedAuthorizationRequest();
        assertThat(request.state()).isEqualTo(STATE);
        assertThat(request.scope()).isEqualTo("read,activity:read");
        assertThat(request.scope()).doesNotContain("activity:read_all");
    }

    @Test
    void execute_returns_generated_authorization_url() {
        stubSuccessfulStart();

        StravaAuthorizationUrlResult result = useCase.execute(startStravaAccountLinkingCommand());

        assertThat(result.authorizationUrl()).isEqualTo(AUTHORIZATION_URL);
    }

    @Test
    void execute_does_not_depend_on_account_link_repository() {
        Field[] fields = StartStravaAccountLinkingUseCaseImpl.class.getDeclaredFields();

        boolean dependsOnAccountLinkRepository = Arrays.stream(fields)
                .anyMatch(field -> field.getType().equals(StravaAccountLinkRepository.class));

        assertThat(dependsOnAccountLinkRepository).isFalse();
    }

    private void stubSuccessfulStart() {
        when(timeProvider.now()).thenReturn(NOW);
        when(stateGenerator.generate()).thenReturn(STATE);
        when(oAuthProvider.buildAuthorizationUrl(capturedAnyRequest())).thenReturn(AUTHORIZATION_URL);
    }

    private StravaAuthorizationRequest capturedAnyRequest() {
        return org.mockito.ArgumentMatchers.any(StravaAuthorizationRequest.class);
    }

    private StravaAuthorizationState capturedSavedState() {
        ArgumentCaptor<StravaAuthorizationState> captor = ArgumentCaptor.forClass(StravaAuthorizationState.class);
        verify(authorizationStateRepository).save(captor.capture());
        return captor.getValue();
    }

    private StravaAuthorizationRequest capturedAuthorizationRequest() {
        ArgumentCaptor<StravaAuthorizationRequest> captor = ArgumentCaptor.forClass(StravaAuthorizationRequest.class);
        verify(oAuthProvider).buildAuthorizationUrl(captor.capture());
        return captor.getValue();
    }
}
