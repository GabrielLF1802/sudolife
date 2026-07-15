package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.exception.CoachingProfileRequiredException;
import com.sudolife.application.service.training.ports.provided.EvaluateRunningGoalUseCase;
import com.sudolife.application.service.training.ports.provided.GenerateAdaptiveRunningPlanUseCase;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.required.AiRunningPlanProvider;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerateAdaptiveRunningPlanUseCaseImpl implements GenerateAdaptiveRunningPlanUseCase {

    private final CoachingProfileRepository coachingProfileRepository;
    private final TrainingProfileRepository trainingProfileRepository;
    private final GetRunningHistorySnapshotUseCase runningHistoryUseCase;
    private final EvaluateRunningGoalUseCase evaluateRunningGoalUseCase;
    private final AiRunningPlanProvider aiRunningPlanProvider;
    private final AiRunningPlanValidator aiRunningPlanValidator;
    private final TimeProvider timeProvider;

    @Override
    public AdaptiveRunningPlanResult execute(String userEmail) {
        CoachingProfile profile = coachingProfileRepository.findByUserEmail(userEmail)
                .orElseThrow(CoachingProfileRequiredException::new);
        RunningHistorySnapshotResult history = runningHistoryUseCase.execute(userEmail);
        RunningGoalAssessmentResult assessment = evaluateRunningGoalUseCase.execute(userEmail);
        List<TrainingHeartRateZone> zones = heartRateZones(userEmail);
        List<PlannedSessionResult> safeSessions = safeSessions(profile, history, zones);
        TrainingSnapshot snapshot = new TrainingSnapshot(history, assessment.safeMilestone(), zones, safeSessions);
        AiRunningPlanProposal proposal = aiRunningPlanProvider.draft(snapshot);
        ValidatedAiRunningPlan validatedPlan = aiRunningPlanValidator.validate(snapshot, proposal);

        return new AdaptiveRunningPlanResult(
                assessment.safeMilestone(),
                validatedPlan.plannedSessions(),
                proposal.explanation(),
                validatedPlan.adjusted());
    }

    private List<TrainingHeartRateZone> heartRateZones(String userEmail) {
        int year = timeProvider.now().atZone(ZoneOffset.UTC).getYear();

        return trainingProfileRepository.findByUserEmail(userEmail)
                .map(profile -> TrainingProfileResult.existing(
                        profile.getBirthYear(), profile.getImportedHeartRateZones(), year).heartRateZones())
                .orElse(List.of());
    }

    private List<PlannedSessionResult> safeSessions(
            CoachingProfile profile,
            RunningHistorySnapshotResult history,
            List<TrainingHeartRateZone> zones
    ) {
        double averageDistance = history.runningActivityCount() == 0 ? 2.0
                : history.totalDistanceKilometers() / history.runningActivityCount();
        double baseDistance = Math.min(profile.getTargetDistanceKilometers() / 1.5,
                Math.max(2.0, averageDistance * 0.8));
        PlannedSessionTargetResult target = zones.isEmpty()
                ? PlannedSessionTargetResult.perceivedEffort(2, 4)
                : PlannedSessionTargetResult.heartRate(zones.getFirst().minimumHeartRate(),
                        zones.get(Math.min(1, zones.size() - 1)).maximumHeartRate());
        LocalDate firstDay = timeProvider.now().atZone(ZoneOffset.UTC).toLocalDate().plusDays(1);
        List<DayOfWeek> days = profile.getRunningAvailability().getPreferredRunningDays().isEmpty()
                ? List.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)
                : profile.getRunningAvailability().getPreferredRunningDays();

        return java.util.stream.IntStream.rangeClosed(1, 4)
                .boxed()
                .flatMap(week -> scheduledDays(days, firstDay, week).stream()
                        .limit(2)
                        .map(date -> session(week, date, baseDistance, target)))
                .toList();
    }

    private List<LocalDate> scheduledDays(List<DayOfWeek> days, LocalDate firstDay, int week) {
        return days.stream()
                .map(day -> firstDay.plusWeeks(week - 1L).with(TemporalAdjusters.nextOrSame(day)))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private PlannedSessionResult session(
            int week,
            LocalDate date,
            double baseDistance,
            PlannedSessionTargetResult target
    ) {
        int sessionNumber = date.getDayOfWeek() == DayOfWeek.SATURDAY ? 2 : 1;
        double distance = baseDistance * Math.pow(1.10, week - 1);
        PlannedSessionType type = sessionNumber == 2 ? PlannedSessionType.LONG_RUN : PlannedSessionType.EASY_RUN;

        return new PlannedSessionResult(week, sessionNumber, type,
                Math.round(distance * (sessionNumber == 2 ? 12.5 : 10.0)) / 10.0, target, date);
    }
}
