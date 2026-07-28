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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrectPlannedSessionMatchUseCaseImplUnitTest {

    @Mock
    private AdaptiveRunningPlanRepository planRepository;

    @Mock
    private StravaActivitySummaryRepository activityRepository;

    @InjectMocks
    private CorrectPlannedSessionMatchUseCaseImpl useCase;

    @Test
    void execute_replaces_wrong_match_with_selected_eligible_activity() {
        AdaptiveRunningPlan plan = plan();
        plan.getPlannedSessions().getFirst().match(10L);
        when(planRepository.findLatestByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(plan));
        when(activityRepository.findByIdAndUserEmail(11L, "runner@sudolife.com"))
                .thenReturn(Optional.of(activity()));
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CurrentAdaptiveRunningPlanResult result = useCase.execute("runner@sudolife.com",
                new CorrectPlannedSessionMatchCommand(2L, 11L));

        assertThat(result.plannedSessions()).singleElement().satisfies(session -> {
            assertThat(session.status()).isEqualTo(PlannedSessionStatus.COMPLETED);
            assertThat(session.matchedActivityId()).isEqualTo(11L);
        });
    }

    private AdaptiveRunningPlan plan() {
        PlannedSessionResult session = new PlannedSessionResult(1, 1, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse("2026-08-03"), 1800);

        return new AdaptiveRunningPlan(1L, "runner@sudolife.com",
                new RunningGoal(10.0, 360, LocalDate.parse("2026-10-01")), "Explanation",
                Instant.parse("2026-07-27T12:00:00Z"),
                List.of(new AdaptiveRunningPlanSession(2L, null, session, PlannedSessionStatus.PLANNED)));
    }

    private StravaActivitySummary activity() {
        return new StravaActivitySummary(11L, "runner@sudolife.com", 20L, 111L, StravaActivityType.RUN,
                "Run", "Evening Run", Instant.parse("2026-08-03T18:00:00Z"), 5050.0, 1810,
                3.0, 360.0, 10.0, 4.0, 150.0, 170.0, 80.0, 200.0, 300.0,
                Instant.parse("2026-08-03T19:00:00Z"));
    }
}
