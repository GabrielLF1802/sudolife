package com.sudolife.application.service.strava.activity;

import com.sudolife.application.service.strava.authorization.StravaAccessTokenService;
import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivityDetailSnapshot;
import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.service.strava.exception.StravaActivityNotFoundException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityDetailSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSyncJobRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Function;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ACTIVITY_START_DATE;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static com.sudolife.helper.StravaTestHelper.stravaActivityDetailImport;
import static com.sudolife.helper.StravaTestHelper.stravaActivityDetailSnapshot;
import static com.sudolife.helper.StravaTestHelper.stravaActivityStreamImport;
import static com.sudolife.helper.StravaTestHelper.stravaActivityStreamSnapshot;
import static com.sudolife.helper.StravaTestHelper.stravaActivitySummary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStravaActivityDetailUseCaseImplUnitTest {

    private static final Long ACTIVITY_ID = 99L;

    @Mock
    private StravaActivitySummaryRepository activitySummaryRepository;

    @Mock
    private StravaActivityDetailSnapshotRepository detailSnapshotRepository;

    @Mock
    private StravaActivityStreamSnapshotRepository streamSnapshotRepository;

    @Mock
    private StravaActivityStreamSyncJobRepository streamSyncJobRepository;

    @Mock
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaActivityProvider activityProvider;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private StravaAccessTokenService accessTokenService;

    @InjectMocks
    private GetStravaActivityDetailUseCaseImpl useCase;

    @Test
    void execute_with_activity_owned_by_user_fetches_and_persists_detail_snapshot() {
        when(activitySummaryRepository.findByIdAndUserEmail(ACTIVITY_ID, USER_EMAIL)).thenReturn(Optional.of(summary()));
        stubAccessTokenService();
        when(detailSnapshotRepository.findByActivitySummaryId(ACTIVITY_ID)).thenReturn(Optional.empty());
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activityProvider.fetchActivityDetail(ACCESS_TOKEN, SOURCE_ACTIVITY_ID)).thenReturn(stravaActivityDetailImport());
        when(timeProvider.now()).thenReturn(NOW);
        when(detailSnapshotRepository.saveIfAbsent(any())).thenReturn(snapshot());
        when(activityProvider.fetchActivityStreams(ACCESS_TOKEN, SOURCE_ACTIVITY_ID)).thenReturn(stravaActivityStreamImport());
        when(streamSnapshotRepository.saveIfAbsent(any())).thenReturn(stravaActivityStreamSnapshot());
        when(streamSnapshotRepository.findByActivitySummaryId(ACTIVITY_ID)).thenReturn(Optional.empty(),
                Optional.of(stravaActivityStreamSnapshot()), Optional.of(stravaActivityStreamSnapshot()));

        StravaActivityDetailResult result = useCase.execute(command());

        StravaActivityDetailSnapshot persistedSnapshot = capturedSnapshot();
        assertThat(result.id()).isEqualTo(ACTIVITY_ID);
        assertThat(result.sourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
        assertThat(result.name()).isEqualTo("Morning Run Detail");
        assertThat(result.averagePaceSecondsPerKilometer()).isCloseTo(296.078431372549, within(0.000001));
        assertThat(result.enrichmentStatus()).isEqualTo(StravaActivityDetailEnrichmentStatus.COMPLETED);
        assertThat(result.streamStatus()).isEqualTo(StravaActivityStreamStatus.COMPLETED);
        assertThat(result.availableStreamMetricNames()).containsExactly("time", "distance", "velocity",
                "heart_rate");
        assertThat(persistedSnapshot.getActivitySummaryId()).isEqualTo(ACTIVITY_ID);
        assertThat(persistedSnapshot.getFetchedAt()).isEqualTo(NOW);
    }

    @Test
    void execute_with_existing_detail_snapshot_returns_cached_snapshot_without_fetching() {
        when(activitySummaryRepository.findByIdAndUserEmail(ACTIVITY_ID, USER_EMAIL)).thenReturn(Optional.of(summary()));
        when(detailSnapshotRepository.findByActivitySummaryId(ACTIVITY_ID)).thenReturn(Optional.of(snapshot()));
        when(streamSnapshotRepository.findByActivitySummaryId(ACTIVITY_ID)).thenReturn(Optional.of(
                stravaActivityStreamSnapshot()));

        StravaActivityDetailResult result = useCase.execute(command());

        assertThat(result.name()).isEqualTo("Morning Run Detail");
        assertThat(result.enrichmentStatus()).isEqualTo(StravaActivityDetailEnrichmentStatus.COMPLETED);
        assertThat(result.streamStatus()).isEqualTo(StravaActivityStreamStatus.COMPLETED);
        verify(activityProvider, never()).fetchActivityDetail(any(), any());
        verify(detailSnapshotRepository, never()).saveIfAbsent(any());
    }

    @Test
    void execute_when_detail_enrichment_fails_returns_summary_fallback() {
        when(activitySummaryRepository.findByIdAndUserEmail(ACTIVITY_ID, USER_EMAIL)).thenReturn(Optional.of(summary()));
        stubAccessTokenService();
        when(detailSnapshotRepository.findByActivitySummaryId(ACTIVITY_ID)).thenReturn(Optional.empty());
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activityProvider.fetchActivityDetail(ACCESS_TOKEN, SOURCE_ACTIVITY_ID))
                .thenThrow(new StravaActivityUnavailableException());

        StravaActivityDetailResult result = useCase.execute(command());

        assertThat(result.name()).isEqualTo("Morning Run");
        assertThat(result.enrichmentStatus()).isEqualTo(StravaActivityDetailEnrichmentStatus.SUMMARY_FALLBACK);
        assertThat(result.availableStreamMetricNames()).isEmpty();
    }

    @Test
    void execute_when_stream_inline_attempt_fails_enqueues_high_priority_stream_job() {
        when(activitySummaryRepository.findByIdAndUserEmail(ACTIVITY_ID, USER_EMAIL)).thenReturn(Optional.of(summary()));
        stubAccessTokenService();
        when(detailSnapshotRepository.findByActivitySummaryId(ACTIVITY_ID)).thenReturn(Optional.empty());
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activityProvider.fetchActivityDetail(ACCESS_TOKEN, SOURCE_ACTIVITY_ID)).thenReturn(stravaActivityDetailImport());
        when(detailSnapshotRepository.saveIfAbsent(any())).thenReturn(snapshot());
        when(activityProvider.fetchActivityStreams(ACCESS_TOKEN, SOURCE_ACTIVITY_ID))
                .thenThrow(new StravaActivityUnavailableException());
        when(timeProvider.now()).thenReturn(NOW);

        StravaActivityDetailResult result = useCase.execute(command());

        StravaActivityStreamSyncJob job = capturedStreamJob();
        assertThat(result.streamStatus()).isEqualTo(StravaActivityStreamStatus.PENDING);
        assertThat(job.getActivitySummaryId()).isEqualTo(ACTIVITY_ID);
        assertThat(job.getPriority().name()).isEqualTo("HIGH");
    }

    @Test
    void execute_with_activity_outside_user_ownership_returns_not_found() {
        when(activitySummaryRepository.findByIdAndUserEmail(ACTIVITY_ID, USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(StravaActivityNotFoundException.class);
    }

    private GetStravaActivityDetailCommand command() {
        return new GetStravaActivityDetailCommand(USER_EMAIL, ACTIVITY_ID);
    }

    private StravaActivitySummary summary() {
        StravaActivitySummary summary = stravaActivitySummary();

        return new StravaActivitySummary(ACTIVITY_ID, summary.getUserEmail(), summary.getAccountLinkId(),
                summary.getSourceActivityId(), summary.getActivityType(), summary.getRawSportType(),
                summary.getName(), summary.getStartDate(), summary.getDistanceMeters(),
                summary.getMovingTimeSeconds(), summary.getAverageSpeedMetersPerSecond(),
                summary.getPaceSecondsPerKilometer(), summary.getTotalElevationGainMeters(),
                summary.getMaxSpeedMetersPerSecond(), summary.getAverageHeartRate(), summary.getMaxHeartRate(),
                summary.getAverageCadence(), summary.getAverageWatts(), summary.getCalories(),
                summary.getImportedAt());
    }

    private StravaActivityDetailSnapshot snapshot() {
        StravaActivityDetailSnapshot snapshot = stravaActivityDetailSnapshot();

        return new StravaActivityDetailSnapshot(7L, ACTIVITY_ID, snapshot.getUserEmail(),
                snapshot.getSourceActivityId(), snapshot.getActivityType(), snapshot.getRawSportType(),
                snapshot.getName(), ACTIVITY_START_DATE, snapshot.getDistanceMeters(),
                snapshot.getMovingTimeSeconds(), snapshot.getAverageSpeedMetersPerSecond(),
                snapshot.getPaceSecondsPerKilometer(), snapshot.getTotalElevationGainMeters(),
                snapshot.getMaxSpeedMetersPerSecond(), snapshot.getAverageHeartRate(),
                snapshot.getMaxHeartRate(), snapshot.getAverageCadence(), snapshot.getAverageWatts(),
                snapshot.getCalories(), snapshot.getFetchedAt());
    }

    private StravaActivityDetailSnapshot capturedSnapshot() {
        ArgumentCaptor<StravaActivityDetailSnapshot> captor = ArgumentCaptor.forClass(StravaActivityDetailSnapshot.class);
        verify(detailSnapshotRepository).saveIfAbsent(captor.capture());

        return captor.getValue();
    }

    private StravaActivityStreamSyncJob capturedStreamJob() {
        ArgumentCaptor<StravaActivityStreamSyncJob> captor = ArgumentCaptor.forClass(StravaActivityStreamSyncJob.class);
        verify(streamSyncJobRepository).enqueueIfAbsent(captor.capture());

        return captor.getValue();
    }

    private void stubAccessTokenService() {
        org.mockito.Mockito.doAnswer(invocation -> {
            StravaAccountLink accountLink = invocation.getArgument(0);
            Function<StravaAccountLink, Object> activityCall = invocation.getArgument(1);

            return activityCall.apply(accountLink);
        }).when(accessTokenService).executeWithValidToken(any(), any());
    }
}
