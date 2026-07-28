package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchImportedRunsUseCaseImplUnitTest {

    @Mock
    private AdaptiveRunningPlanRepository planRepository;

    @Mock
    private StravaActivitySummaryRepository activityRepository;

    @InjectMocks
    private MatchImportedRunsUseCaseImpl useCase;

    @Test
    void execute_with_eligible_run_completes_planned_session() {
        AdaptiveRunningPlan plan = plan();
        when(planRepository.findLatestByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(plan));
        when(activityRepository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq("runner@sudolife.com"), eq(StravaActivityType.RUN), any(), any()))
                .thenReturn(List.of(activity(10L, 5100.0, 1820)));
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute("runner@sudolife.com");

        AdaptiveRunningPlan savedPlan = capturedPlan();
        assertThat(savedPlan.getPlannedSessions()).singleElement().satisfies(session -> {
            assertThat(session.getStatus()).isEqualTo(PlannedSessionStatus.COMPLETED);
            assertThat(session.getMatchedActivityId()).isEqualTo(10L);
        });
    }

    @Test
    void execute_without_eligible_run_does_not_change_plan() {
        AdaptiveRunningPlan plan = plan();
        when(planRepository.findLatestByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(plan));
        when(activityRepository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq("runner@sudolife.com"), eq(StravaActivityType.RUN), any(), any()))
                .thenReturn(List.of(activity(10L, 7000.0, 2500)));

        useCase.execute("runner@sudolife.com");

        verify(planRepository, never()).save(any());
        assertThat(plan.getPlannedSessions().getFirst().getStatus()).isEqualTo(PlannedSessionStatus.PLANNED);
    }

    private AdaptiveRunningPlan capturedPlan() {
        ArgumentCaptor<AdaptiveRunningPlan> captor = ArgumentCaptor.forClass(AdaptiveRunningPlan.class);
        verify(planRepository).save(captor.capture());

        return captor.getValue();
    }

    private AdaptiveRunningPlan plan() {
        PlannedSessionResult plannedSession = new PlannedSessionResult(1, 1, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse("2026-08-03"), 1800);

        return new AdaptiveRunningPlan(1L, "runner@sudolife.com",
                new RunningGoal(10.0, 360, LocalDate.parse("2026-10-01")), "Explanation",
                Instant.parse("2026-07-27T12:00:00Z"),
                List.of(new AdaptiveRunningPlanSession(2L, null, plannedSession, PlannedSessionStatus.PLANNED)));
    }

    private StravaActivitySummary activity(Long id, double distanceMeters, int durationSeconds) {
        return new StravaActivitySummary(id, "runner@sudolife.com", 20L, id + 100, StravaActivityType.RUN,
                "Run", "Morning Run", Instant.parse("2026-08-03T09:00:00Z"), distanceMeters, durationSeconds,
                3.0, 360.0, 10.0, 4.0, 150.0, 170.0, 80.0, 200.0, 300.0,
                Instant.parse("2026-08-03T10:00:00Z"));
    }
}
