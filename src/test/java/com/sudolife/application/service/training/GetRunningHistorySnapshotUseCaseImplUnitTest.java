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

    private StravaActivitySummary run(long daysAgo, double distanceMeters) {
        return StravaActivitySummary.imported(USER_EMAIL, 1L, daysAgo, StravaActivityType.RUN, "Run", "Run",
                NOW.minusSeconds(daysAgo * 86400), distanceMeters, 1800, 3.0, 10.0, 4.0,
                null, null, null, null, null, NOW);
    }
}
