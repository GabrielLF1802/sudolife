package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivityStreamSnapshot;
import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;
import com.sudolife.application.model.strava.StravaActivityStreamSyncJobPriority;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityStreamUnavailableException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static com.sudolife.helper.StravaTestHelper.stravaActivityStreamImport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProcessStravaActivityStreamSyncJobUseCaseImplUnitTest {

    private static final Long JOB_ID = 77L;
    private static final Long ACTIVITY_ID = 99L;

    @Mock
    private StravaActivityStreamSyncJobRepository streamSyncJobRepository;

    @Mock
    private StravaActivityStreamSnapshotRepository streamSnapshotRepository;

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaActivityProvider activityProvider;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private StravaAccessTokenService accessTokenService;

    @InjectMocks
    private ProcessStravaActivityStreamSyncJobUseCaseImpl useCase;

    @Test
    void execute_with_stream_job_persists_snapshot_and_completes_job() {
        when(streamSyncJobRepository.findById(JOB_ID)).thenReturn(Optional.of(queuedJob()));
        stubAccessTokenService();
        when(streamSyncJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountLinkRepository.findActiveById(LINK_ID)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activityProvider.fetchActivityStreams(ACCESS_TOKEN, SOURCE_ACTIVITY_ID)).thenReturn(stravaActivityStreamImport());
        when(timeProvider.now()).thenReturn(NOW);

        useCase.execute(new ProcessStravaActivityStreamSyncJobCommand(JOB_ID));

        StravaActivityStreamSnapshot snapshot = capturedSnapshot();
        StravaActivityStreamSyncJob completedJob = lastSavedJob();
        assertThat(snapshot.getAvailableMetricNames()).containsExactly("time", "distance", "velocity", "heart_rate");
        assertThat(snapshot.getStreamSamplesJson()).doesNotContain("latlng");
        assertThat(completedJob.getStatus()).isEqualTo(StravaSummarySyncJobStatus.COMPLETED);
    }

    @Test
    void execute_when_rate_limited_schedules_retry_with_backoff() {
        ReflectionTestUtils.setField(useCase, "retryBackoff", Duration.ofMinutes(15));
        ReflectionTestUtils.setField(useCase, "maxAttempts", 3);
        stubAccessTokenService();
        when(streamSyncJobRepository.findById(JOB_ID)).thenReturn(Optional.of(queuedJob()));
        when(streamSyncJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountLinkRepository.findActiveById(LINK_ID)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activityProvider.fetchActivityStreams(ACCESS_TOKEN, SOURCE_ACTIVITY_ID))
                .thenThrow(new StravaActivityRateLimitException());
        when(timeProvider.now()).thenReturn(NOW);

        useCase.execute(new ProcessStravaActivityStreamSyncJobCommand(JOB_ID));

        StravaActivityStreamSyncJob retryJob = lastSavedJob();
        assertThat(retryJob.getStatus()).isEqualTo(StravaSummarySyncJobStatus.QUEUED);
        assertThat(retryJob.getFailureReason()).isEqualTo(StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED.name());
        assertThat(retryJob.getRunAfter()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void execute_when_strava_has_no_stream_samples_marks_job_as_failed() {
        stubAccessTokenService();
        when(streamSyncJobRepository.findById(JOB_ID)).thenReturn(Optional.of(queuedJob()));
        when(streamSyncJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountLinkRepository.findActiveById(LINK_ID)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activityProvider.fetchActivityStreams(ACCESS_TOKEN, SOURCE_ACTIVITY_ID))
                .thenThrow(new StravaActivityStreamUnavailableException("No samples"));
        when(timeProvider.now()).thenReturn(NOW);

        useCase.execute(new ProcessStravaActivityStreamSyncJobCommand(JOB_ID));

        StravaActivityStreamSyncJob failedJob = lastSavedJob();
        assertThat(failedJob.getStatus()).isEqualTo(StravaSummarySyncJobStatus.FAILED);
        assertThat(failedJob.getFailureReason()).isEqualTo(StravaActivitySyncFailureReason.NO_STREAMS_AVAILABLE.name());
        verify(streamSnapshotRepository, never()).saveIfAbsent(any());
    }

    private StravaActivityStreamSyncJob queuedJob() {
        return new StravaActivityStreamSyncJob(JOB_ID, ACTIVITY_ID, LINK_ID, USER_EMAIL, SOURCE_ACTIVITY_ID,
                StravaActivityStreamSyncJobPriority.NORMAL, StravaSummarySyncJobStatus.QUEUED, 0, NOW, null,
                null, null, NOW, NOW);
    }

    private StravaActivityStreamSnapshot capturedSnapshot() {
        ArgumentCaptor<StravaActivityStreamSnapshot> captor = ArgumentCaptor.forClass(StravaActivityStreamSnapshot.class);
        verify(streamSnapshotRepository).saveIfAbsent(captor.capture());

        return captor.getValue();
    }

    private StravaActivityStreamSyncJob lastSavedJob() {
        ArgumentCaptor<StravaActivityStreamSyncJob> captor = ArgumentCaptor.forClass(StravaActivityStreamSyncJob.class);
        verify(streamSyncJobRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        return captor.getAllValues().getLast();
    }

    private void stubAccessTokenService() {
        org.mockito.Mockito.doAnswer(invocation -> {
            StravaAccountLink accountLink = invocation.getArgument(0);
            Function<StravaAccountLink, Object> activityCall = invocation.getArgument(1);

            return activityCall.apply(accountLink);
        }).when(accessTokenService).executeWithValidToken(any(), any());
    }
}
