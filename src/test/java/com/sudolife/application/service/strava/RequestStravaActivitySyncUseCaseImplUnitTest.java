package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestStravaActivitySyncUseCaseImplUnitTest {

    private static final Long SOURCE_ACTIVITY_ID = 457L;
    private static final Instant START_DATE = Instant.parse("2026-05-10T09:00:00Z");
    private static final Instant INITIAL_SYNC_AFTER = Instant.parse("2025-10-11T12:00:00Z");

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaActivityProvider activityProvider;

    @Mock
    private StravaActivitySummaryRepository activitySummaryRepository;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private RequestStravaActivitySyncUseCaseImpl useCase;

    @Test
    void execute_with_sync_enabled_link_imports_initial_window_summaries() {
        stubTransaction();
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(activityProvider.fetchActivitySummaries(ACCESS_TOKEN, INITIAL_SYNC_AFTER, NOW))
                .thenReturn(List.of(activitySummaryImport()));
        when(activitySummaryRepository.saveIfAbsent(any())).thenReturn(true);
        when(activitySummaryRepository.countByUserEmail(USER_EMAIL)).thenReturn(1L);

        StravaActivitySyncResult result = useCase.execute(command());

        StravaActivitySummary savedSummary = capturedSummary();
        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.COMPLETED);
        assertThat(result.failureReason()).isNull();
        assertThat(result.importedActivityCount()).isEqualTo(1);
        assertThat(result.totalActivityCount()).isEqualTo(1);
        assertThat(savedSummary.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(savedSummary.getAccountLinkId()).isEqualTo(LINK_ID);
        assertThat(savedSummary.getSourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
        assertThat(savedSummary.getActivityType()).isEqualTo(StravaActivityType.RUN);
        assertThat(savedSummary.getRawSportType()).isEqualTo("Run");
        assertThat(savedSummary.getPaceSecondsPerKilometer()).isEqualTo(300.0);
    }

    @Test
    void execute_with_existing_summary_does_not_count_duplicate_import() {
        stubTransaction();
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(activityProvider.fetchActivitySummaries(ACCESS_TOKEN, INITIAL_SYNC_AFTER, NOW))
                .thenReturn(List.of(activitySummaryImport()));
        when(activitySummaryRepository.saveIfAbsent(any())).thenReturn(false);
        when(activitySummaryRepository.countByUserEmail(USER_EMAIL)).thenReturn(1L);

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.COMPLETED);
        assertThat(result.importedActivityCount()).isZero();
        assertThat(result.totalActivityCount()).isEqualTo(1);
    }

    @Test
    void execute_with_permission_deficient_link_returns_upgrade_required_without_strava_call() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(readOnlyLink()));

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED);
        verify(activityProvider, never()).fetchActivitySummaries(any(), any(), any());
    }

    @Test
    void execute_without_active_link_returns_unlinked_without_strava_call() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.UNLINKED);
        assertThat(result.failureReason()).isNull();
        verify(activityProvider, never()).fetchActivitySummaries(any(), any(), any());
    }

    @Test
    void execute_when_strava_rate_limits_returns_safe_failure_reason() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(activityProvider.fetchActivitySummaries(ACCESS_TOKEN, INITIAL_SYNC_AFTER, NOW))
                .thenThrow(new StravaActivityRateLimitException());

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED);
        verify(activitySummaryRepository, never()).saveIfAbsent(any());
    }

    @Test
    void execute_when_strava_rate_limits_after_partial_fetch_preserves_imported_progress() {
        stubTransaction();
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(activityProvider.fetchActivitySummaries(ACCESS_TOKEN, INITIAL_SYNC_AFTER, NOW))
                .thenThrow(new StravaActivityRateLimitException(List.of(activitySummaryImport())));
        when(activitySummaryRepository.saveIfAbsent(any())).thenReturn(true);
        when(activitySummaryRepository.countByUserEmail(USER_EMAIL)).thenReturn(1L);

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED);
        assertThat(result.importedActivityCount()).isEqualTo(1);
        assertThat(result.totalActivityCount()).isEqualTo(1);
        assertThat(capturedSummary().getSourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
    }

    @Test
    void execute_when_strava_is_unavailable_returns_safe_failure_reason() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(activityProvider.fetchActivitySummaries(ACCESS_TOKEN, INITIAL_SYNC_AFTER, NOW))
                .thenThrow(new StravaActivityUnavailableException());

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.STRAVA_UNAVAILABLE);
        verify(activitySummaryRepository, never()).saveIfAbsent(any());
    }

    private RequestStravaActivitySyncCommand command() {
        return new RequestStravaActivitySyncCommand(USER_EMAIL);
    }

    private StravaActivitySummaryImport activitySummaryImport() {
        return new StravaActivitySummaryImport(SOURCE_ACTIVITY_ID, StravaActivityType.RUN, "Run",
                "Morning Run", START_DATE, 5000.0, 1500, 3.33, 42.0, 5.5, 150.0, 180.0,
                82.0, 220.0, 350.0);
    }

    private StravaAccountLink readOnlyLink() {
        return StravaAccountLink.active(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT,
                "read", LINKED_AT);
    }

    private StravaActivitySummary capturedSummary() {
        ArgumentCaptor<StravaActivitySummary> captor = ArgumentCaptor.forClass(StravaActivitySummary.class);
        verify(activitySummaryRepository).saveIfAbsent(captor.capture());

        return captor.getValue();
    }

    private void stubTransaction() {
        doAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);

            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
    }
}
