package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaImportedDataRepository;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static com.sudolife.helper.StravaTestHelper.unlinkStravaAccountCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnlinkStravaAccountUseCaseImplUnitTest {

    private static final String ROTATED_ACCESS_TOKEN = "rotated-access-token";
    private static final String ROTATED_REFRESH_TOKEN = "rotated-refresh-token";
    private static final Instant EXPIRED_AT = Instant.parse("2026-05-11T11:00:00Z");

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaImportedDataRepository importedDataRepository;

    @Mock
    private StravaOAuthProvider oAuthProvider;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private UnlinkStravaAccountUseCaseImpl useCase;

    @Test
    void execute_with_active_link_deauthorizes_and_marks_link_inactive() {
        stubActiveLink(activeStravaAccountLink());

        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        StravaAccountLink savedLink = capturedSavedLink();
        assertThat(result.unlinked()).isTrue();
        assertThat(savedLink.isInactive()).isTrue();
        assertThat(savedLink.getUnlinkedAt()).isEqualTo(NOW);
        assertThat(savedLink.getAccessToken()).isNull();
        assertThat(savedLink.getRefreshToken()).isNull();
        assertThat(savedLink.getExpiresAt()).isNull();
        InOrder unlinkOrder = inOrder(importedDataRepository, accountLinkRepository, oAuthProvider);
        unlinkOrder.verify(importedDataRepository).deleteByAccountLinkId(LINK_ID);
        unlinkOrder.verify(accountLinkRepository).save(any());
        unlinkOrder.verify(oAuthProvider).deauthorize(ACCESS_TOKEN);
    }

    @Test
    void execute_without_active_link_is_idempotent() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());

        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        assertThat(result.unlinked()).isTrue();
        verify(oAuthProvider, never()).deauthorize(any());
        verify(accountLinkRepository, never()).save(any());
        verify(importedDataRepository, never()).deleteByAccountLinkId(any());
    }

    @Test
    void execute_with_expired_access_token_refreshes_before_deauthorization() {
        StravaAccountLink expiredLink = activeLink(EXPIRED_AT);
        StravaTokenAuthorization refreshed = new StravaTokenAuthorization(ATHLETE_ID, ROTATED_ACCESS_TOKEN,
                ROTATED_REFRESH_TOKEN, EXPIRES_AT, SCOPE);
        stubActiveLink(expiredLink);
        when(oAuthProvider.refresh(REFRESH_TOKEN)).thenReturn(refreshed);

        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        assertThat(result.unlinked()).isTrue();
        verify(oAuthProvider).refresh(REFRESH_TOKEN);
        verify(oAuthProvider).deauthorize(ROTATED_ACCESS_TOKEN);
        assertThat(capturedSavedLink().isInactive()).isTrue();
    }

    @Test
    void execute_when_deauthorization_fails_still_marks_link_inactive() {
        stubActiveLink(activeStravaAccountLink());
        doThrow(new RuntimeException("strava unavailable")).when(oAuthProvider).deauthorize(ACCESS_TOKEN);

        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        assertThat(result.unlinked()).isTrue();
        assertThat(capturedSavedLink().isInactive()).isTrue();
    }

    @Test
    void execute_when_local_unlink_fails_does_not_deauthorize() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        doThrow(new RuntimeException("persistence unavailable")).when(transactionTemplate).executeWithoutResult(any());

        assertThatThrownBy(() -> useCase.execute(unlinkStravaAccountCommand()))
                .hasMessage("persistence unavailable");

        verify(oAuthProvider, never()).deauthorize(any());
    }

    @Test
    void execute_when_refresh_fails_still_marks_link_inactive_without_deauthorization() {
        StravaAccountLink expiredLink = activeLink(EXPIRED_AT);
        stubActiveLink(expiredLink);
        when(oAuthProvider.refresh(REFRESH_TOKEN)).thenThrow(new RuntimeException("strava unavailable"));

        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        assertThat(result.unlinked()).isTrue();
        assertThat(capturedSavedLink().isInactive()).isTrue();
        verify(oAuthProvider, never()).deauthorize(any());
    }

    @Test
    void result_does_not_include_token_fields() {
        String[] componentNames = Arrays.stream(UnlinkStravaAccountResult.class.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);

        assertThat(componentNames).containsExactly("unlinked");
    }

    private void stubActiveLink(StravaAccountLink accountLink) {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(accountLink));
        when(timeProvider.now()).thenReturn(NOW);
        doAnswer(invocation -> {
            Consumer<Object> callback = invocation.getArgument(0);
            callback.accept(null);

            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private StravaAccountLink activeLink(Instant expiresAt) {
        return StravaAccountLink.active(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, expiresAt,
                LINKED_AT);
    }

    private StravaAccountLink capturedSavedLink() {
        ArgumentCaptor<StravaAccountLink> captor = ArgumentCaptor.forClass(StravaAccountLink.class);
        verify(accountLinkRepository).save(captor.capture());
        return captor.getValue();
    }
}
