package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.CODE;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static com.sudolife.helper.StravaTestHelper.completeStravaAccountLinkingCommand;
import static com.sudolife.helper.StravaTestHelper.stravaTokenAuthorization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteStravaAccountLinkingUseCaseImplUnitTest {

    private static final String OTHER_USER_EMAIL = "other@sudolife.com";
    private static final String ROTATED_ACCESS_TOKEN = "rotated-access-token";
    private static final String ROTATED_REFRESH_TOKEN = "rotated-refresh-token";
    private static final Instant ROTATED_EXPIRES_AT = Instant.parse("2026-05-12T18:00:00Z");

    @Mock
    private StravaAuthorizationStateRepository authorizationStateRepository;

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaOAuthProvider oAuthProvider;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private CompleteStravaAccountLinkingUseCaseImpl useCase;

    @Test
    void execute_with_valid_callback_creates_active_link_and_consumes_state() {
        stubSuccessfulTokenExchange();

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        StravaAccountLink savedLink = capturedSavedLink();
        assertThat(result.linked()).isTrue();
        assertThat(savedLink.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(savedLink.getAthleteId()).isEqualTo(ATHLETE_ID);
        assertThat(savedLink.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(savedLink.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(savedLink.getExpiresAt()).isEqualTo(EXPIRES_AT);
        verify(authorizationStateRepository).consumePending(STATE, NOW, NOW);
    }

    @Test
    void execute_with_missing_state_returns_invalid_state() {
        when(timeProvider.now()).thenReturn(NOW);
        CompleteStravaAccountLinkingCommand command = new CompleteStravaAccountLinkingCommand(" ", CODE, "read", null);

        StravaCallbackResult result = useCase.execute(command);

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INVALID_STATE");
        verify(oAuthProvider, never()).exchangeAuthorizationCode(any());
        verify(accountLinkRepository, never()).save(any());
    }

    @Test
    void execute_with_unknown_state_returns_invalid_state() {
        when(timeProvider.now()).thenReturn(NOW);
        when(authorizationStateRepository.consumePending(STATE, NOW, NOW)).thenReturn(Optional.empty());

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INVALID_STATE");
        verify(oAuthProvider, never()).exchangeAuthorizationCode(any());
    }

    @Test
    void execute_with_expired_state_returns_invalid_state() {
        when(timeProvider.now()).thenReturn(NOW);
        when(authorizationStateRepository.consumePending(STATE, NOW, NOW)).thenReturn(Optional.empty());

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INVALID_STATE");
        verify(oAuthProvider, never()).exchangeAuthorizationCode(any());
    }

    @Test
    void execute_with_consumed_state_returns_invalid_state() {
        when(timeProvider.now()).thenReturn(NOW);
        when(authorizationStateRepository.consumePending(STATE, NOW, NOW)).thenReturn(Optional.empty());

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INVALID_STATE");
        verify(oAuthProvider, never()).exchangeAuthorizationCode(any());
    }

    @Test
    void execute_with_denied_authorization_consumes_state_without_token_exchange() {
        stubPendingState();
        CompleteStravaAccountLinkingCommand command = new CompleteStravaAccountLinkingCommand(STATE, null, null,
                "access_denied");

        StravaCallbackResult result = useCase.execute(command);

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("AUTHORIZATION_DENIED");
        verify(authorizationStateRepository).consumePending(STATE, NOW, NOW);
        verify(oAuthProvider, never()).exchangeAuthorizationCode(any());
        verify(accountLinkRepository, never()).save(any());
    }

    @Test
    void execute_when_token_exchange_fails_consumes_state_and_returns_failure() {
        stubPendingState();
        when(oAuthProvider.exchangeAuthorizationCode(CODE)).thenThrow(new RuntimeException("unavailable"));

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("TOKEN_EXCHANGE_FAILED");
        verify(authorizationStateRepository).consumePending(STATE, NOW, NOW);
        verify(accountLinkRepository, never()).save(any());
    }

    @Test
    void execute_with_missing_read_scope_consumes_state_without_linking() {
        stubPendingState();
        when(oAuthProvider.exchangeAuthorizationCode(CODE)).thenReturn(stravaTokenAuthorization());
        CompleteStravaAccountLinkingCommand command = new CompleteStravaAccountLinkingCommand(STATE, CODE,
                "profile:read_all activity:read", null);

        StravaCallbackResult result = useCase.execute(command);

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INSUFFICIENT_SCOPE");
        verify(authorizationStateRepository).consumePending(STATE, NOW, NOW);
        verify(accountLinkRepository, never()).save(any());
    }

    @Test
    void execute_with_space_delimited_read_scope_links_account() {
        stubPendingState();
        when(oAuthProvider.exchangeAuthorizationCode(CODE)).thenReturn(stravaTokenAuthorization());
        CompleteStravaAccountLinkingCommand command = new CompleteStravaAccountLinkingCommand(STATE, CODE,
                "profile:read_all read activity:read", null);

        StravaCallbackResult result = useCase.execute(command);

        assertThat(result.linked()).isTrue();
        assertThat(capturedSavedLink().getAthleteId()).isEqualTo(ATHLETE_ID);
    }

    @Test
    void execute_with_athlete_linked_to_another_user_returns_duplicate_failure() {
        stubPendingState();
        when(oAuthProvider.exchangeAuthorizationCode(CODE)).thenReturn(stravaTokenAuthorization());
        when(accountLinkRepository.findActiveByAthleteId(ATHLETE_ID))
                .thenReturn(Optional.of(activeLinkFor(OTHER_USER_EMAIL)));

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("ATHLETE_ALREADY_LINKED");
        verify(accountLinkRepository, never()).save(any());
    }

    @Test
    void execute_with_same_user_reconnect_replaces_token_metadata() {
        stubPendingState();
        StravaTokenAuthorization token = new StravaTokenAuthorization(ATHLETE_ID, ROTATED_ACCESS_TOKEN,
                ROTATED_REFRESH_TOKEN, ROTATED_EXPIRES_AT, "read");
        when(oAuthProvider.exchangeAuthorizationCode(CODE)).thenReturn(token);
        when(accountLinkRepository.findActiveByAthleteId(ATHLETE_ID)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        StravaAccountLink savedLink = capturedSavedLink();
        assertThat(result.linked()).isTrue();
        assertThat(savedLink.getId()).isEqualTo(LINK_ID);
        assertThat(savedLink.getAccessToken()).isEqualTo(ROTATED_ACCESS_TOKEN);
        assertThat(savedLink.getRefreshToken()).isEqualTo(ROTATED_REFRESH_TOKEN);
        assertThat(savedLink.getExpiresAt()).isEqualTo(ROTATED_EXPIRES_AT);
    }

    @Test
    void execute_when_persistence_race_creates_duplicate_returns_duplicate_failure() {
        stubPendingState();
        when(oAuthProvider.exchangeAuthorizationCode(CODE)).thenReturn(stravaTokenAuthorization());
        when(accountLinkRepository.save(any())).thenThrow(new DuplicateStravaAthleteOwnershipException());

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("ATHLETE_ALREADY_LINKED");
    }

    private void stubSuccessfulTokenExchange() {
        stubPendingState();
        when(oAuthProvider.exchangeAuthorizationCode(CODE)).thenReturn(stravaTokenAuthorization());
    }

    private void stubPendingState() {
        when(timeProvider.now()).thenReturn(NOW);
        when(authorizationStateRepository.consumePending(STATE, NOW, NOW))
                .thenReturn(Optional.of(new StravaAuthorizationState(STATE, USER_EMAIL, EXPIRES_AT, NOW)));
        lenient().doAnswer(invocation -> {
            Consumer<Object> callback = invocation.getArgument(0);
            callback.accept(null);

            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private StravaAccountLink activeLinkFor(String userEmail) {
        return StravaAccountLink.active(LINK_ID, userEmail, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, NOW);
    }

    private StravaAccountLink capturedSavedLink() {
        ArgumentCaptor<StravaAccountLink> captor = ArgumentCaptor.forClass(StravaAccountLink.class);
        verify(accountLinkRepository).save(captor.capture());
        return captor.getValue();
    }
}
