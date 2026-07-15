package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetRunningHistorySnapshotUseCaseImplUnitTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final String USER_EMAIL = "runner@sudolife.com";

    private final StravaActivitySummaryRepository repository = mock(StravaActivitySummaryRepository.class);
    private final TimeProvider timeProvider = () -> NOW;
    private final GetRunningHistorySnapshotUseCaseImpl useCase =
            new GetRunningHistorySnapshotUseCaseImpl(repository, timeProvider);

    @Test
    void execute_with_runs_in_three_of_the_last_four_weeks_returns_sufficient_history() {
        when(repository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq(USER_EMAIL), eq(StravaActivityType.RUN), any(), eq(NOW)))
                .thenReturn(List.of(run(2, 5000.0), run(9, 6000.0), run(16, 7000.0)));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.sufficientRunningHistory()).isTrue();
        assertThat(result.activeWeeks()).isEqualTo(3);
        assertThat(result.runningActivityCount()).isEqualTo(3);
        assertThat(result.totalDistanceKilometers()).isEqualTo(18.0);
        assertThat(result.totalMovingTimeSeconds()).isEqualTo(5400);
        assertThat(result.weeklyRunningVolumes()).hasSize(12);
        assertThat(result.averageRunsPerWeek()).isEqualTo(0.25);
        assertThat(result.longestRunKilometers()).isEqualTo(7.0);
        assertThat(result.representativePaceSecondsPerKilometer()).isEqualTo(300.0);
    }

    @Test
    void execute_with_sparse_running_history_returns_explicit_insufficient_history() {
        when(repository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq(USER_EMAIL), eq(StravaActivityType.RUN), any(), eq(NOW)))
                .thenReturn(List.of(run(2, 5000.0), run(3, 4000.0)));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.sufficientRunningHistory()).isFalse();
        assertThat(result.activeWeeks()).isEqualTo(1);
    }

    @Test
    void execute_queries_only_imported_run_activities() {
        when(repository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq(USER_EMAIL), eq(StravaActivityType.RUN), any(), eq(NOW))).thenReturn(List.of());

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.runningActivityCount()).isZero();
        assertThat(result.latestRunAt()).isNull();
    }

    @Test
    void execute_with_older_running_experience_keeps_recent_history_insufficient() {
        when(repository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq(USER_EMAIL), eq(StravaActivityType.RUN), any(), eq(NOW)))
                .thenReturn(List.of(run(2, 5000.0), run(37, 8000.0), run(51, 10000.0)));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.sufficientRunningHistory()).isFalse();
        assertThat(result.activeWeeks()).isEqualTo(1);
        assertThat(result.runningActivityCount()).isEqualTo(3);
        assertThat(result.totalDistanceKilometers()).isEqualTo(23.0);
        assertThat(result.longestRunKilometers()).isEqualTo(10.0);
    }

    @Test
    void execute_with_increasing_recent_volume_returns_increasing_trend() {
        when(repository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq(USER_EMAIL), eq(StravaActivityType.RUN), any(), eq(NOW)))
                .thenReturn(List.of(run(2, 8000.0), run(9, 8000.0), run(16, 8000.0), run(23, 8000.0),
                        run(30, 4000.0), run(37, 4000.0), run(44, 4000.0), run(51, 4000.0)));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.volumeTrend()).isEqualTo(RunningVolumeTrend.INCREASING);
        assertThat(result.weeklyRunningVolumes().getFirst().distanceKilometers()).isEqualTo(8.0);
    }

    @Test
    void execute_with_missing_distance_and_time_produces_available_metrics() {
        when(repository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq(USER_EMAIL), eq(StravaActivityType.RUN), any(), eq(NOW)))
                .thenReturn(List.of(run(2, null, 1800), run(9, 6000.0, null), run(16, 5000.0, 1500)));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.sufficientRunningHistory()).isTrue();
        assertThat(result.totalDistanceKilometers()).isEqualTo(11.0);
        assertThat(result.totalMovingTimeSeconds()).isEqualTo(3300);
        assertThat(result.representativePaceSecondsPerKilometer()).isEqualTo(300.0);
    }

    @Test
    void execute_with_no_complete_pace_metrics_returns_snapshot_without_representative_pace() {
        when(repository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq(USER_EMAIL), eq(StravaActivityType.RUN), any(), eq(NOW)))
                .thenReturn(List.of(run(2, null, 1800), run(9, 6000.0, null)));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.runningActivityCount()).isEqualTo(2);
        assertThat(result.representativePaceSecondsPerKilometer()).isNull();
    }

    private StravaActivitySummary run(long daysAgo, double distanceMeters) {
        return run(daysAgo, distanceMeters, 1800);
    }

    private StravaActivitySummary run(long daysAgo, Double distanceMeters, Integer movingTimeSeconds) {
        return StravaActivitySummary.imported(USER_EMAIL, 1L, daysAgo, StravaActivityType.RUN, "Run", "Run",
                NOW.minusSeconds(daysAgo * 86400), distanceMeters, movingTimeSeconds, 3.0, 10.0, 4.0,
                null, null, null, null, null, NOW);
    }
}
