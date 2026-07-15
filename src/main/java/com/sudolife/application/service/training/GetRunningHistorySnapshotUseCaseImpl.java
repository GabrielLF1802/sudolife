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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GetRunningHistorySnapshotUseCaseImpl implements GetRunningHistorySnapshotUseCase {

    private static final int RECENT_HISTORY_WEEKS = 4;
    private static final int PLANNING_HISTORY_WEEKS = 12;
    private static final int SUFFICIENT_ACTIVE_WEEKS = 3;
    private static final double STABLE_TREND_TOLERANCE = 0.10;
    private static final Duration WEEK = Duration.ofDays(7);

    private final StravaActivitySummaryRepository activityRepository;
    private final TimeProvider timeProvider;

    @Override
    public RunningHistorySnapshotResult execute(String userEmail) {
        Instant now = timeProvider.now();
        Instant historyStart = now.minus(WEEK.multipliedBy(PLANNING_HISTORY_WEEKS));
        List<StravaActivitySummary> runs = activityRepository
                .findByUserEmailAndActivityTypeAndStartDateBetween(
                        userEmail, StravaActivityType.RUN, historyStart, now)
                .stream()
                .filter(run -> weeksAgo(run, now) < PLANNING_HISTORY_WEEKS)
                .toList();
        int activeWeeks = activeWeeks(runs, now);
        List<WeeklyRunningVolumeResult> weeklyVolumes = weeklyVolumes(runs, now);
        double totalDistanceKilometers = totalDistanceKilometers(runs);
        long totalMovingTimeSeconds = totalMovingTimeSeconds(runs);

        return new RunningHistorySnapshotResult(
                activeWeeks >= SUFFICIENT_ACTIVE_WEEKS,
                activeWeeks,
                runs.size(),
                totalDistanceKilometers,
                totalMovingTimeSeconds,
                latestRunAt(runs),
                weeklyVolumes,
                (double) runs.size() / PLANNING_HISTORY_WEEKS,
                longestRunKilometers(runs),
                representativePace(runs),
                volumeTrend(weeklyVolumes)
        );
    }

    private int activeWeeks(List<StravaActivitySummary> runs, Instant now) {
        return (int) runs.stream()
                .map(run -> Duration.between(run.getStartDate(), now).toDays() / 7)
                .filter(week -> week >= 0 && week < RECENT_HISTORY_WEEKS)
                .distinct()
                .count();
    }

    private List<WeeklyRunningVolumeResult> weeklyVolumes(List<StravaActivitySummary> runs, Instant now) {
        return IntStream.range(0, PLANNING_HISTORY_WEEKS)
                .mapToObj(weeksAgo -> weeklyVolume(runs, now, weeksAgo))
                .toList();
    }

    private WeeklyRunningVolumeResult weeklyVolume(
            List<StravaActivitySummary> runs,
            Instant now,
            int weeksAgo
    ) {
        List<StravaActivitySummary> runsInWeek = runs.stream()
                .filter(run -> weeksAgo(run, now) == weeksAgo)
                .toList();

        return new WeeklyRunningVolumeResult(
                weeksAgo,
                runsInWeek.size(),
                totalDistanceKilometers(runsInWeek),
                totalMovingTimeSeconds(runsInWeek));
    }

    private long weeksAgo(StravaActivitySummary run, Instant now) {
        return Duration.between(run.getStartDate(), now).toDays() / 7;
    }

    private double totalDistanceKilometers(List<StravaActivitySummary> runs) {
        return runs.stream()
                .map(StravaActivitySummary::getDistanceMeters)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum() / 1000.0;
    }

    private long totalMovingTimeSeconds(List<StravaActivitySummary> runs) {
        return runs.stream()
                .map(StravaActivitySummary::getMovingTimeSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private Instant latestRunAt(List<StravaActivitySummary> runs) {
        return runs.stream()
                .map(StravaActivitySummary::getStartDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private double longestRunKilometers(List<StravaActivitySummary> runs) {
        return runs.stream()
                .map(StravaActivitySummary::getDistanceMeters)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0) / 1000.0;
    }

    private Double representativePace(List<StravaActivitySummary> runs) {
        List<StravaActivitySummary> runsWithPace = runs.stream()
                .filter(run -> run.getDistanceMeters() != null && run.getDistanceMeters() > 0)
                .filter(run -> run.getMovingTimeSeconds() != null && run.getMovingTimeSeconds() > 0)
                .toList();
        double distanceKilometers = totalDistanceKilometers(runsWithPace);
        long movingTimeSeconds = totalMovingTimeSeconds(runsWithPace);

        if (distanceKilometers <= 0 || movingTimeSeconds <= 0) {
            return null;
        }

        return movingTimeSeconds / distanceKilometers;
    }

    private RunningVolumeTrend volumeTrend(List<WeeklyRunningVolumeResult> weeklyVolumes) {
        double recentVolume = averageWeeklyDistance(weeklyVolumes.subList(0, RECENT_HISTORY_WEEKS));
        double previousVolume = averageWeeklyDistance(
                weeklyVolumes.subList(RECENT_HISTORY_WEEKS, RECENT_HISTORY_WEEKS * 2));

        if (recentVolume == 0 || previousVolume == 0) {
            return RunningVolumeTrend.INSUFFICIENT_DATA;
        }

        double change = (recentVolume - previousVolume) / previousVolume;

        if (change > STABLE_TREND_TOLERANCE) {
            return RunningVolumeTrend.INCREASING;
        }

        if (change < -STABLE_TREND_TOLERANCE) {
            return RunningVolumeTrend.DECREASING;
        }

        return RunningVolumeTrend.STABLE;
    }

    private double averageWeeklyDistance(List<WeeklyRunningVolumeResult> weeklyVolumes) {
        return weeklyVolumes.stream()
                .mapToDouble(WeeklyRunningVolumeResult::distanceKilometers)
                .average()
                .orElse(0);
    }
}
