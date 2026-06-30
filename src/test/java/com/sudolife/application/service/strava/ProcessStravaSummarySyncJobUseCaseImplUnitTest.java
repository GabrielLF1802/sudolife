package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSyncJobRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessStravaSummarySyncJobUseCaseImplUnitTest {

    private static final Long JOB_ID = 55L;
    private static final Instant INITIAL_SYNC_AFTER = Instant.parse("2025-10-11T12:00:00Z");

    @Mock
    private StravaSummarySyncJobRepository summarySyncJobRepository;

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaActivityProvider activityProvider;

    @Mock
    private StravaActivitySummaryRepository activitySummaryRepository;

    @Mock
    private StravaActivityStreamSyncJobRepository streamSyncJobRepository;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ProcessStravaSummarySyncJobUseCaseImpl useCase;

    @Test
    void execute_with_summary_job_imports_summaries_and_completes_job() {
        stubTransaction();
        when(summarySyncJobRepository.findById(JOB_ID)).thenReturn(Optional.of(queuedJob()));
        when(summarySyncJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountLinkRepository.findActiveById(LINK_ID)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(activityProvider.fetchActivitySummaries(ACCESS_TOKEN, INITIAL_SYNC_AFTER, NOW))
                .thenReturn(List.of(activitySummaryImport()));
        when(activitySummaryRepository.saveIfAbsent(any())).thenReturn(true);
        when(activitySummaryRepository.findByUserEmailAndSourceActivityId(USER_EMAIL, SOURCE_ACTIVITY_ID))
                .thenReturn(Optional.of(savedSummary()));

        useCase.execute(new ProcessStravaSummarySyncJobCommand(JOB_ID));

        StravaActivitySummary savedSummary = capturedSummary();
        StravaSummarySyncJob completedJob = lastSavedJob();
        assertThat(savedSummary.getSourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
        assertThat(completedJob.getStatus()).isEqualTo(StravaSummarySyncJobStatus.COMPLETED);
        assertThat(completedJob.getImportedActivityCount()).isEqualTo(1);
    }

    @Test
    void execute_when_rate_limited_preserves_partial_import_and_schedules_retry() {
        stubTransaction();
        ReflectionTestUtils.setField(useCase, "retryBackoff", Duration.ofMinutes(15));
        ReflectionTestUtils.setField(useCase, "maxAttempts", 3);
        when(summarySyncJobRepository.findById(JOB_ID)).thenReturn(Optional.of(queuedJob()));
        when(summarySyncJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountLinkRepository.findActiveById(LINK_ID)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(timeProvider.now()).thenReturn(NOW);
        when(activityProvider.fetchActivitySummaries(ACCESS_TOKEN, INITIAL_SYNC_AFTER, NOW))
                .thenThrow(new StravaActivityRateLimitException(List.of(activitySummaryImport())));
        when(activitySummaryRepository.saveIfAbsent(any())).thenReturn(true);
        when(activitySummaryRepository.findByUserEmailAndSourceActivityId(USER_EMAIL, SOURCE_ACTIVITY_ID))
                .thenReturn(Optional.of(savedSummary()));

        useCase.execute(new ProcessStravaSummarySyncJobCommand(JOB_ID));

        StravaSummarySyncJob retryJob = lastSavedJob();
        assertThat(retryJob.getStatus()).isEqualTo(StravaSummarySyncJobStatus.QUEUED);
        assertThat(retryJob.getFailureReason()).isEqualTo(StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED.name());
        assertThat(retryJob.getImportedActivityCount()).isEqualTo(1);
        assertThat(retryJob.getRunAfter()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(capturedSummary().getSourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
    }

    private StravaSummarySyncJob queuedJob() {
        return new StravaSummarySyncJob(JOB_ID, LINK_ID, USER_EMAIL, StravaSummarySyncJobStatus.QUEUED, 0, 0,
                NOW, null, null, null, NOW, NOW);
    }

    private StravaActivitySummaryImport activitySummaryImport() {
        return new StravaActivitySummaryImport(SOURCE_ACTIVITY_ID, StravaActivityType.RUN, "Run",
                "Morning Run", Instant.parse("2026-05-10T09:00:00Z"), 5000.0, 1500, 3.33, 42.0, 5.5,
                150.0, 180.0, 82.0, 220.0, 350.0);
    }

    private StravaActivitySummary capturedSummary() {
        ArgumentCaptor<StravaActivitySummary> captor = ArgumentCaptor.forClass(StravaActivitySummary.class);
        verify(activitySummaryRepository).saveIfAbsent(captor.capture());

        return captor.getValue();
    }

    private StravaActivitySummary savedSummary() {
        StravaActivitySummary summary = capturedFriendlySummary();

        return new StravaActivitySummary(99L, summary.getUserEmail(), summary.getAccountLinkId(),
                summary.getSourceActivityId(), summary.getActivityType(), summary.getRawSportType(),
                summary.getName(), summary.getStartDate(), summary.getDistanceMeters(),
                summary.getMovingTimeSeconds(), summary.getAverageSpeedMetersPerSecond(),
                summary.getPaceSecondsPerKilometer(), summary.getTotalElevationGainMeters(),
                summary.getMaxSpeedMetersPerSecond(), summary.getAverageHeartRate(), summary.getMaxHeartRate(),
                summary.getAverageCadence(), summary.getAverageWatts(), summary.getCalories(),
                summary.getImportedAt());
    }

    private StravaActivitySummary capturedFriendlySummary() {
        return StravaActivitySummary.imported(USER_EMAIL, LINK_ID, SOURCE_ACTIVITY_ID, StravaActivityType.RUN, "Run",
                "Morning Run", Instant.parse("2026-05-10T09:00:00Z"), 5000.0, 1500, 3.33, 42.0, 5.5,
                150.0, 180.0, 82.0, 220.0, 350.0, NOW);
    }

    private StravaSummarySyncJob lastSavedJob() {
        ArgumentCaptor<StravaSummarySyncJob> captor = ArgumentCaptor.forClass(StravaSummarySyncJob.class);
        verify(summarySyncJobRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        return captor.getAllValues().getLast();
    }

    private void stubTransaction() {
        doAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);

            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
    }
}
