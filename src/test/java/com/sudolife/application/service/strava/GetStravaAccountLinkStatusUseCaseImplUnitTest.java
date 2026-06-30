package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
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
import static com.sudolife.helper.StravaTestHelper.getStravaAccountLinkStatusCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStravaAccountLinkStatusUseCaseImplUnitTest {

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaSummarySyncJobRepository summarySyncJobRepository;

    @Mock
    private StravaActivitySummaryRepository activitySummaryRepository;

    @Mock
    private StravaActivityStreamSnapshotRepository streamSnapshotRepository;

    @InjectMocks
    private GetStravaAccountLinkStatusUseCaseImpl useCase;

    @Test
    void execute_with_active_link_returns_linked_status_with_athlete_id() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.linked()).isTrue();
        assertThat(result.athleteId()).isEqualTo(ATHLETE_ID);
        assertThat(result.permissionState()).isEqualTo(StravaPermissionState.READY);
        assertThat(result.activitySummaryStatus()).isEqualTo(StravaSummaryStatus.NOT_STARTED);
        assertThat(result.performanceDataStatus()).isEqualTo(StravaPerformanceDataStatus.NOT_STARTED);
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void execute_with_read_only_link_returns_permission_upgrade_required() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(readOnlyLink()));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.linked()).isTrue();
        assertThat(result.athleteId()).isEqualTo(ATHLETE_ID);
        assertThat(result.permissionState()).isEqualTo(StravaPermissionState.PERMISSION_UPGRADE_REQUIRED);
        assertThat(result.activitySummaryStatus()).isEqualTo(StravaSummaryStatus.PERMISSION_UPGRADE_REQUIRED);
        assertThat(result.performanceDataStatus()).isEqualTo(StravaPerformanceDataStatus.PERMISSION_UPGRADE_REQUIRED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED);
    }

    @Test
    void execute_without_active_link_returns_unlinked_status_without_athlete_id_or_counts() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.athleteId()).isNull();
        assertThat(result.permissionState()).isEqualTo(StravaPermissionState.UNLINKED);
        assertThat(result.activitySummaryStatus()).isEqualTo(StravaSummaryStatus.UNLINKED);
        assertThat(result.performanceDataStatus()).isEqualTo(StravaPerformanceDataStatus.UNLINKED);
        assertThat(result.importedActivityCount()).isZero();
        assertThat(result.streamsReadyActivityCount()).isZero();
    }

    @Test
    void execute_with_queued_sync_returns_running_safe_failure_reason() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(summarySyncJobRepository.findLatestByAccountLinkId(LINK_ID)).thenReturn(Optional.of(queuedJob()));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.activitySummaryStatus()).isEqualTo(StravaSummaryStatus.QUEUED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.SYNC_ALREADY_RUNNING);
    }

    @Test
    void execute_with_queued_sync_preserves_previous_completed_sync_time() {
        StravaSummarySyncJob completedJob = queuedJob().completed(2, NOW.minusSeconds(60));
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(summarySyncJobRepository.findLatestByAccountLinkId(LINK_ID)).thenReturn(Optional.of(queuedJob()));
        when(summarySyncJobRepository.findLatestCompletedByAccountLinkId(LINK_ID)).thenReturn(Optional.of(completedJob));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.activitySummaryStatus()).isEqualTo(StravaSummaryStatus.QUEUED);
        assertThat(result.lastSummarySyncTime()).isEqualTo(NOW.minusSeconds(60));
    }

    @Test
    void execute_with_running_sync_returns_running_safe_failure_reason() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(summarySyncJobRepository.findLatestByAccountLinkId(LINK_ID)).thenReturn(Optional.of(
                queuedJob().running(NOW)));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.activitySummaryStatus()).isEqualTo(StravaSummaryStatus.RUNNING);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.SYNC_ALREADY_RUNNING);
    }

    @Test
    void execute_with_rate_limited_failure_returns_stable_safe_failure_reason() {
        StravaSummarySyncJob failedJob = queuedJob().permanentFailure("STRAVA_RATE_LIMITED", 2, NOW);
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(summarySyncJobRepository.findLatestByAccountLinkId(LINK_ID)).thenReturn(Optional.of(failedJob));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.activitySummaryStatus()).isEqualTo(StravaSummaryStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED);
        assertThat(result.lastSummarySyncTime()).isNull();
    }

    @Test
    void execute_with_unknown_failure_returns_unknown_sync_failure() {
        StravaSummarySyncJob failedJob = queuedJob().permanentFailure("PROVIDER_CHANGED", 2, NOW);
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(summarySyncJobRepository.findLatestByAccountLinkId(LINK_ID)).thenReturn(Optional.of(failedJob));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.failureReason()).isEqualTo(StravaActivitySyncFailureReason.UNKNOWN_SYNC_FAILURE);
    }

    @Test
    void execute_with_completed_sync_returns_import_counts_and_last_summary_sync_time() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activitySummaryRepository.countByAccountLinkId(LINK_ID)).thenReturn(4L);
        when(activitySummaryRepository.countStreamsReadyByAccountLinkId(LINK_ID)).thenReturn(1L);
        StravaSummarySyncJob completedJob = queuedJob().completed(4, NOW);
        when(summarySyncJobRepository.findLatestByAccountLinkId(LINK_ID)).thenReturn(Optional.of(completedJob));
        when(summarySyncJobRepository.findLatestCompletedByAccountLinkId(LINK_ID)).thenReturn(Optional.of(completedJob));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.activitySummaryStatus()).isEqualTo(StravaSummaryStatus.COMPLETED);
        assertThat(result.performanceDataStatus()).isEqualTo(StravaPerformanceDataStatus.PENDING);
        assertThat(result.lastSummarySyncTime()).isEqualTo(NOW);
        assertThat(result.importedActivityCount()).isEqualTo(4);
        assertThat(result.streamsReadyActivityCount()).isEqualTo(1);
    }

    @Test
    void execute_with_all_imported_activities_stream_ready_returns_ready_performance_status() {
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activitySummaryRepository.countByAccountLinkId(LINK_ID)).thenReturn(2L);
        when(activitySummaryRepository.countStreamsReadyByAccountLinkId(LINK_ID)).thenReturn(2L);

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.performanceDataStatus()).isEqualTo(StravaPerformanceDataStatus.READY);
    }

    @Test
    void result_does_not_include_token_fields() {
        String[] componentNames = Arrays.stream(StravaLinkStatusResult.class.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);

        assertThat(componentNames).containsExactly("linked", "athleteId", "permissionState", "activitySummaryStatus",
                "performanceDataStatus", "lastSummarySyncTime", "lastStreamEnrichmentTime",
                "importedActivityCount", "streamsReadyActivityCount", "failureReason");
    }

    private StravaAccountLink readOnlyLink() {
        return StravaAccountLink.active(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT,
                "read", LINKED_AT);
    }

    private StravaSummarySyncJob queuedJob() {
        return StravaSummarySyncJob.queued(activeStravaAccountLink(), NOW);
    }
}
