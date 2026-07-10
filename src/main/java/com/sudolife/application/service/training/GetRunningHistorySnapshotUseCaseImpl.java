package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRunningHistorySnapshotUseCaseImpl implements GetRunningHistorySnapshotUseCase {

    private static final int HISTORY_WEEKS = 4;
    private static final int SUFFICIENT_ACTIVE_WEEKS = 3;
    private static final Duration WEEK = Duration.ofDays(7);

    private final StravaActivitySummaryRepository activityRepository;
    private final TimeProvider timeProvider;

    @Override
    public RunningHistorySnapshotResult execute(String userEmail) {
        Instant now = timeProvider.now();
        Instant historyStart = now.minus(WEEK.multipliedBy(HISTORY_WEEKS));
        List<StravaActivitySummary> runs = activityRepository
                .findByUserEmailAndActivityTypeAndStartDateBetween(
                        userEmail, StravaActivityType.RUN, historyStart, now);
        int activeWeeks = activeWeeks(runs, now);

        return new RunningHistorySnapshotResult(
                activeWeeks >= SUFFICIENT_ACTIVE_WEEKS,
                activeWeeks,
                runs.size(),
                runs.stream().map(StravaActivitySummary::getDistanceMeters).filter(java.util.Objects::nonNull)
                        .mapToDouble(Double::doubleValue).sum() / 1000.0,
                runs.stream().map(StravaActivitySummary::getMovingTimeSeconds).filter(java.util.Objects::nonNull)
                        .mapToLong(Integer::longValue).sum(),
                runs.isEmpty() ? null : runs.getFirst().getStartDate()
        );
    }

    private int activeWeeks(List<StravaActivitySummary> runs, Instant now) {
        return (int) runs.stream()
                .map(run -> Duration.between(run.getStartDate(), now).toDays() / 7)
                .filter(week -> week >= 0 && week < HISTORY_WEEKS)
                .distinct()
                .count();
    }
}
