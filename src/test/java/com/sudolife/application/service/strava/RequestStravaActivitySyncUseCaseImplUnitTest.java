package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static com.sudolife.helper.StravaTestHelper.reconnectRequiredStravaAccountLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestStravaActivitySyncUseCaseImplUnitTest {

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaActivitySummaryRepository activitySummaryRepository;

    @Mock
    private StravaSummarySyncJobRepository summarySyncJobRepository;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private RequestStravaActivitySyncUseCaseImpl useCase;

    @Test
    void execute_with_sync_enabled_link_queues_summary_job() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(summarySyncJobRepository.enqueueIfAbsent(any())).thenReturn(true);
        when(activitySummaryRepository.countByUserEmail(USER_EMAIL)).thenReturn(7L);

        StravaActivitySyncResult result = useCase.execute(command());

        StravaSummarySyncJob queuedJob = capturedQueuedJob();
        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.COMPLETED);
        assertThat(result.failureReason()).isNull();
        assertThat(result.importedActivityCount()).isZero();
        assertThat(result.totalActivityCount()).isEqualTo(7);
        assertThat(queuedJob.getAccountLinkId()).isEqualTo(LINK_ID);
        assertThat(queuedJob.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(queuedJob.getRunAfter()).isEqualTo(NOW);
    }

    @Test
    void execute_with_existing_queued_or_running_job_returns_already_running() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(summarySyncJobRepository.enqueueIfAbsent(any())).thenReturn(false);

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.SYNC_ALREADY_RUNNING);
        assertThat(result.importedActivityCount()).isZero();
    }

    @Test
    void execute_with_permission_deficient_link_returns_upgrade_required_without_queueing() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(readOnlyLink()));

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED);
        verify(summarySyncJobRepository, never()).enqueueIfAbsent(any());
    }

    @Test
    void execute_with_reconnect_required_link_returns_reconnect_required_without_queueing() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL))
                .thenReturn(Optional.of(reconnectRequiredStravaAccountLink()));

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.RECONNECT_REQUIRED);
        verify(summarySyncJobRepository, never()).enqueueIfAbsent(any());
    }

    @Test
    void execute_without_active_link_returns_unlinked_without_queueing() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());

        StravaActivitySyncResult result = useCase.execute(command());

        assertThat(result.status()).isEqualTo(StravaActivitySyncStatus.UNLINKED);
        assertThat(result.failureReason()).isNull();
        verify(summarySyncJobRepository, never()).enqueueIfAbsent(any());
    }

    private RequestStravaActivitySyncCommand command() {
        return new RequestStravaActivitySyncCommand(USER_EMAIL);
    }

    private StravaAccountLink readOnlyLink() {
        return StravaAccountLink.active(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT,
                "read", LINKED_AT);
    }

    private StravaSummarySyncJob capturedQueuedJob() {
        ArgumentCaptor<StravaSummarySyncJob> captor = ArgumentCaptor.forClass(StravaSummarySyncJob.class);
        verify(summarySyncJobRepository).enqueueIfAbsent(captor.capture());

        return captor.getValue();
    }
}
