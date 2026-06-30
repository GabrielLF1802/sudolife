package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaActivityDetailSnapshot;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.service.strava.exception.StravaActivityNotFoundException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityDetailSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ACTIVITY_START_DATE;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static com.sudolife.helper.StravaTestHelper.stravaActivityDetailImport;
import static com.sudolife.helper.StravaTestHelper.stravaActivityDetailSnapshot;
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
    private StravaAccountLinkRepository accountLinkRepository;

    @Mock
    private StravaActivityProvider activityProvider;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private GetStravaActivityDetailUseCaseImpl useCase;

    @Test
    void execute_with_activity_owned_by_user_fetches_and_persists_detail_snapshot() {
        when(activitySummaryRepository.findByIdAndUserEmail(ACTIVITY_ID, USER_EMAIL)).thenReturn(Optional.of(summary()));
        when(detailSnapshotRepository.findByActivitySummaryId(ACTIVITY_ID)).thenReturn(Optional.empty());
        when(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).thenReturn(Optional.of(activeStravaAccountLink()));
        when(activityProvider.fetchActivityDetail(ACCESS_TOKEN, SOURCE_ACTIVITY_ID)).thenReturn(stravaActivityDetailImport());
        when(timeProvider.now()).thenReturn(NOW);
        when(detailSnapshotRepository.saveIfAbsent(any())).thenReturn(snapshot());

        StravaActivityDetailResult result = useCase.execute(command());

        StravaActivityDetailSnapshot persistedSnapshot = capturedSnapshot();
        assertThat(result.id()).isEqualTo(ACTIVITY_ID);
        assertThat(result.sourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
        assertThat(result.name()).isEqualTo("Morning Run Detail");
        assertThat(result.averagePaceSecondsPerKilometer()).isCloseTo(296.078431372549, within(0.000001));
        assertThat(result.enrichmentStatus()).isEqualTo(StravaActivityDetailEnrichmentStatus.COMPLETED);
        assertThat(persistedSnapshot.getActivitySummaryId()).isEqualTo(ACTIVITY_ID);
        assertThat(persistedSnapshot.getFetchedAt()).isEqualTo(NOW);
    }

    @Test
    void execute_with_existing_detail_snapshot_returns_cached_snapshot_without_fetching() {
        when(activitySummaryRepository.findByIdAndUserEmail(ACTIVITY_ID, USER_EMAIL)).thenReturn(Optional.of(summary()));
        when(detailSnapshotRepository.findByActivitySummaryId(ACTIVITY_ID)).thenReturn(Optional.of(snapshot()));

        StravaActivityDetailResult result = useCase.execute(command());

        assertThat(result.name()).isEqualTo("Morning Run Detail");
        assertThat(result.enrichmentStatus()).isEqualTo(StravaActivityDetailEnrichmentStatus.COMPLETED);
        verify(activityProvider, never()).fetchActivityDetail(any(), any());
        verify(detailSnapshotRepository, never()).saveIfAbsent(any());
    }

    @Test
    void execute_when_detail_enrichment_fails_returns_summary_fallback() {
        when(activitySummaryRepository.findByIdAndUserEmail(ACTIVITY_ID, USER_EMAIL)).thenReturn(Optional.of(summary()));
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
}
