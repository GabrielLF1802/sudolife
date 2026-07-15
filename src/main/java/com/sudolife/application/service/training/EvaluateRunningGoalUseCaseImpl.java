package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.exception.CoachingProfileRequiredException;
import com.sudolife.application.service.training.ports.provided.EvaluateRunningGoalUseCase;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluateRunningGoalUseCaseImpl implements EvaluateRunningGoalUseCase {

    private static final int MILESTONE_WEEKS = 4;
    private static final double STANDARD_WEEKLY_DISTANCE_PROGRESSION = 1.05;
    private static final double SUFFICIENT_HISTORY_WEEKLY_DISTANCE_PROGRESSION = 1.10;
    private static final double STANDARD_WEEKLY_PACE_IMPROVEMENT = 0.98;
    private static final double DEFAULT_STARTING_DISTANCE_KILOMETERS = 2.0;

    private final CoachingProfileRepository coachingProfileRepository;
    private final GetRunningHistorySnapshotUseCase runningHistoryUseCase;
    private final TimeProvider timeProvider;

    @Override
    public RunningGoalAssessmentResult execute(String userEmail) {
        CoachingProfile profile = coachingProfileRepository.findByUserEmail(userEmail)
                .orElseThrow(CoachingProfileRequiredException::new);
        RunningHistorySnapshotResult history = runningHistoryUseCase.execute(userEmail);
        LocalDate currentDate = timeProvider.now().atZone(ZoneOffset.UTC).toLocalDate();
        RunningGoal goal = profile.getRunningGoal();
        List<RunningGoalAssessmentReason> reasons = reasons(profile, history, currentDate);
        RunningGoalResult longTermGoal = result(goal);

        if (reasons.isEmpty()) {
            return new RunningGoalAssessmentResult(true, List.of(), longTermGoal, longTermGoal);
        }

        return new RunningGoalAssessmentResult(
                false,
                reasons,
                longTermGoal,
                safeMilestone(profile, history, currentDate)
        );
    }

    private List<RunningGoalAssessmentReason> reasons(
            CoachingProfile profile,
            RunningHistorySnapshotResult history,
            LocalDate currentDate
    ) {
        RunningGoal goal = profile.getRunningGoal();
        int evaluationWeeks = evaluationWeeks(goal.getTargetDate(), currentDate);
        List<RunningGoalAssessmentReason> reasons = new ArrayList<>();

        if (goal.getTargetDistanceKilometers() > safeDistance(profile, history, evaluationWeeks)) {
            reasons.add(RunningGoalAssessmentReason.UNREALISTIC_DISTANCE);
        }

        Integer safePace = safePace(profile, history, evaluationWeeks);
        if (goal.getTargetPaceSecondsPerKilometer() != null
                && (safePace == null || goal.getTargetPaceSecondsPerKilometer() < safePace)) {
            reasons.add(RunningGoalAssessmentReason.UNREALISTIC_PACE);
        }

        if (goal.getTargetDate() != null && goal.getTargetDate().isBefore(currentDate.plusWeeks(MILESTONE_WEEKS))) {
            reasons.add(RunningGoalAssessmentReason.UNREALISTIC_TARGET_DATE);
        }

        return List.copyOf(reasons);
    }

    private RunningGoalResult safeMilestone(
            CoachingProfile profile,
            RunningHistorySnapshotResult history,
            LocalDate currentDate
    ) {
        RunningGoal goal = profile.getRunningGoal();
        double distance = Math.min(
                goal.getTargetDistanceKilometers(),
                safeDistance(profile, history, MILESTONE_WEEKS));
        Integer safePace = safePace(profile, history, MILESTONE_WEEKS);
        Integer pace = milestonePace(goal.getTargetPaceSecondsPerKilometer(), safePace);

        return new RunningGoalResult(roundDistance(distance), pace, currentDate.plusWeeks(MILESTONE_WEEKS));
    }

    private double safeDistance(
            CoachingProfile profile,
            RunningHistorySnapshotResult history,
            int weeks
    ) {
        double startingDistance = history.runningActivityCount() == 0
                ? DEFAULT_STARTING_DISTANCE_KILOMETERS
                : history.totalDistanceKilometers() / history.runningActivityCount();
        double weeklyProgression = distanceProgression(profile, history);

        return startingDistance * Math.pow(weeklyProgression, weeks);
    }

    private double distanceProgression(CoachingProfile profile, RunningHistorySnapshotResult history) {
        if (profile.getReadiness() == UserReportedReadiness.LOW || profile.isInjuryConcern()) {
            return 1.0;
        }

        return history.sufficientRunningHistory()
                ? SUFFICIENT_HISTORY_WEEKLY_DISTANCE_PROGRESSION
                : STANDARD_WEEKLY_DISTANCE_PROGRESSION;
    }

    private Integer safePace(
            CoachingProfile profile,
            RunningHistorySnapshotResult history,
            int weeks
    ) {
        if (history.representativePaceSecondsPerKilometer() == null) {
            return null;
        }

        double currentPace = history.representativePaceSecondsPerKilometer();
        double weeklyImprovement = profile.getReadiness() == UserReportedReadiness.LOW || profile.isInjuryConcern()
                ? 1.0
                : STANDARD_WEEKLY_PACE_IMPROVEMENT;

        return (int) Math.round(currentPace * Math.pow(weeklyImprovement, weeks));
    }

    private int evaluationWeeks(LocalDate targetDate, LocalDate currentDate) {
        if (targetDate == null || targetDate.isBefore(currentDate.plusWeeks(MILESTONE_WEEKS))) {
            return MILESTONE_WEEKS;
        }

        return Math.max(MILESTONE_WEEKS, (int) ChronoUnit.WEEKS.between(currentDate, targetDate));
    }

    private RunningGoalResult result(RunningGoal goal) {
        return new RunningGoalResult(
                goal.getTargetDistanceKilometers(),
                goal.getTargetPaceSecondsPerKilometer(),
                goal.getTargetDate()
        );
    }

    private double roundDistance(double distance) {
        return Math.round(distance * 10.0) / 10.0;
    }

    private Integer milestonePace(Integer targetPace, Integer safePace) {
        if (targetPace == null || safePace == null) {
            return null;
        }

        return Math.max(targetPace, safePace);
    }
}
