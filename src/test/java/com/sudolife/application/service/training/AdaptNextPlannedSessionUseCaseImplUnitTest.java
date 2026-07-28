package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdaptNextPlannedSessionUseCaseImplUnitTest {

    @ParameterizedTest
    @EnumSource(AdaptationTrigger.class)
    void execute_with_each_trigger_replaces_only_the_next_planned_session(AdaptationTrigger trigger) {
        AdaptiveRunningPlanRepository repository = repositoryWith(plan());
        AdaptNextPlannedSessionUseCaseImpl useCase = useCase(repository);

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com", new AdaptNextPlannedSessionCommand(trigger));

        assertThat(result.plannedSessions()).hasSize(3);
        assertThat(result.plannedSessions().get(0).status()).isEqualTo(PlannedSessionStatus.REPLACED);
        assertThat(result.plannedSessions().get(1).plannedSession()).isEqualTo(laterSession());
        assertThat(result.plannedSessions().get(1).status()).isEqualTo(PlannedSessionStatus.PLANNED);
        assertThat(result.plannedSessions().get(2).originalPlannedSessionId()).isEqualTo(10L);
        assertThat(result.plannedSessions().get(2).adaptationTrigger()).isEqualTo(trigger);
    }

    @Test
    void execute_with_active_injury_overrides_progression_trigger_with_recovery_session() {
        AdaptiveRunningPlanRepository repository = repositoryWith(plan());
        CoachingProfileRepository profileRepository = mock(CoachingProfileRepository.class);
        when(profileRepository.findByUserEmail("runner@sudolife.com"))
                .thenReturn(Optional.of(injuryProfile()));
        AdaptNextPlannedSessionUseCaseImpl useCase = useCase(repository, profileRepository);

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com", new AdaptNextPlannedSessionCommand(AdaptationTrigger.COMPLETED_PLANNED_SESSION));

        assertThat(result.plannedSessions().get(2).adaptationTrigger()).isEqualTo(AdaptationTrigger.INJURY_CONCERN);
        assertThat(result.plannedSessions().get(2).plannedSession().type()).isEqualTo(PlannedSessionType.RECOVERY);
        assertThat(result.plannedSessions().get(2).plannedSession().distanceKilometers()).isZero();
        assertThat(result.plannedSessions().get(2).plannedSession().target())
                .isEqualTo(PlannedSessionTargetResult.perceivedEffort(1, 3));
    }

    @Test
    void execute_with_reported_high_effort_prefers_report_over_inferred_low_effort() {
        AdaptiveRunningPlan plan = planWithCompletedSession(9);
        AdaptiveRunningPlanRepository repository = repositoryWith(plan);
        StravaActivitySummaryRepository activityRepository = activityRepositoryWith(fastActivity());
        AdaptNextPlannedSessionUseCaseImpl useCase = useCase(repository,
                mock(CoachingProfileRepository.class), activityRepository);

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com", completedSessionCommand());

        assertThat(result.plannedSessions().getLast().adaptationTrigger())
                .isEqualTo(AdaptationTrigger.UNEXPECTEDLY_HIGH_EFFORT);
        assertThat(result.plannedSessions().getLast().plannedSession().distanceKilometers()).isEqualTo(3.5);
    }

    @Test
    void execute_without_reported_effort_uses_conservative_inferred_low_effort() {
        AdaptiveRunningPlan plan = planWithCompletedSession(null);
        AdaptiveRunningPlanRepository repository = repositoryWith(plan);
        AdaptNextPlannedSessionUseCaseImpl useCase = useCase(repository,
                mock(CoachingProfileRepository.class), activityRepositoryWith(fastActivity()));

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com", completedSessionCommand());

        assertThat(result.plannedSessions().getLast().adaptationTrigger())
                .isEqualTo(AdaptationTrigger.UNEXPECTEDLY_LOW_EFFORT);
        assertThat(result.plannedSessions().getLast().plannedSession().distanceKilometers()).isEqualTo(5.3);
    }

    @Test
    void execute_with_injury_concern_overrides_reported_low_effort() {
        AdaptiveRunningPlan plan = planWithCompletedSession(1);
        AdaptiveRunningPlanRepository repository = repositoryWith(plan);
        CoachingProfileRepository profileRepository = mock(CoachingProfileRepository.class);
        when(profileRepository.findByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(injuryProfile()));
        AdaptNextPlannedSessionUseCaseImpl useCase = useCase(repository, profileRepository,
                activityRepositoryWith(fastActivity()));

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com", completedSessionCommand());

        assertThat(result.plannedSessions().getLast().adaptationTrigger())
                .isEqualTo(AdaptationTrigger.INJURY_CONCERN);
    }

    private AdaptNextPlannedSessionUseCaseImpl useCase(AdaptiveRunningPlanRepository repository) {
        return useCase(repository, mock(CoachingProfileRepository.class));
    }

    private AdaptNextPlannedSessionUseCaseImpl useCase(
            AdaptiveRunningPlanRepository repository,
            CoachingProfileRepository profileRepository
    ) {
        return useCase(repository, profileRepository, mock(StravaActivitySummaryRepository.class));
    }

    private AdaptNextPlannedSessionUseCaseImpl useCase(
            AdaptiveRunningPlanRepository repository,
            CoachingProfileRepository profileRepository,
            StravaActivitySummaryRepository activityRepository
    ) {
        TimeProvider timeProvider = () -> Instant.parse("2026-07-27T12:00:00Z");

        return new AdaptNextPlannedSessionUseCaseImpl(
                repository, profileRepository, activityRepository, new PostSessionEffortAssessment(),
                new AdaptedPlannedSessionValidator(), timeProvider);
    }

    private AdaptiveRunningPlanRepository repositoryWith(AdaptiveRunningPlan plan) {
        AdaptiveRunningPlanRepository repository = mock(AdaptiveRunningPlanRepository.class);
        when(repository.findLatestByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(plan));
        when(repository.save(plan)).thenReturn(plan);

        return repository;
    }

    private AdaptiveRunningPlan plan() {
        return new AdaptiveRunningPlan(
                1L,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                "Accepted explanation",
                Instant.parse("2026-07-20T12:00:00Z"),
                List.of(
                        new AdaptiveRunningPlanSession(10L, null, nextSession(), PlannedSessionStatus.PLANNED),
                        new AdaptiveRunningPlanSession(11L, null, laterSession(), PlannedSessionStatus.PLANNED)));
    }

    private AdaptiveRunningPlan planWithCompletedSession(Integer reportedEffort) {
        PlannedSessionResult completedResult = new PlannedSessionResult(
                1, 0, PlannedSessionType.EASY_RUN, 5.0, PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse("2026-07-26"), 1800);
        AdaptiveRunningPlanSession completed = new AdaptiveRunningPlanSession(
                9L, null, completedResult, PlannedSessionStatus.COMPLETED,
                null, 30L, reportedEffort);

        return new AdaptiveRunningPlan(
                1L,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                "Accepted explanation",
                Instant.parse("2026-07-20T12:00:00Z"),
                List.of(completed,
                        new AdaptiveRunningPlanSession(10L, null, nextSession(), PlannedSessionStatus.PLANNED),
                        new AdaptiveRunningPlanSession(11L, null, laterSession(), PlannedSessionStatus.PLANNED)));
    }

    private AdaptNextPlannedSessionCommand completedSessionCommand() {
        return new AdaptNextPlannedSessionCommand(AdaptationTrigger.COMPLETED_PLANNED_SESSION);
    }

    private StravaActivitySummaryRepository activityRepositoryWith(StravaActivitySummary activity) {
        StravaActivitySummaryRepository repository = mock(StravaActivitySummaryRepository.class);
        when(repository.findByIdAndUserEmail(30L, "runner@sudolife.com")).thenReturn(Optional.of(activity));

        return repository;
    }

    private StravaActivitySummary fastActivity() {
        return new StravaActivitySummary(30L, "runner@sudolife.com", 20L, 100L, StravaActivityType.RUN,
                "Run", "Fast run", Instant.parse("2026-07-26T09:00:00Z"), 5000.0, 1400,
                3.5, 280.0, 10.0, 4.0, 140.0, 160.0, 80.0, 200.0, 300.0,
                Instant.parse("2026-07-26T10:00:00Z"));
    }

    private CoachingProfile injuryProfile() {
        return new CoachingProfile(
                1L,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                UserReportedReadiness.HIGH,
                true);
    }

    private PlannedSessionResult nextSession() {
        return session(1, 1, 5.0, "2026-07-28");
    }

    private PlannedSessionResult laterSession() {
        return session(1, 2, 7.0, "2026-08-01");
    }

    private PlannedSessionResult session(int week, int number, double distance, String date) {
        return new PlannedSessionResult(
                week,
                number,
                PlannedSessionType.EASY_RUN,
                distance,
                PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse(date));
    }
}
