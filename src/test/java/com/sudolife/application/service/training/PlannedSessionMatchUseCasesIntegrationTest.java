package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.training.ports.provided.CorrectPlannedSessionMatchUseCase;
import com.sudolife.application.service.training.ports.provided.MatchImportedRunsUseCase;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.provided.UnlinkPlannedSessionMatchUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "adaptive-coaching.missed-session-scheduling-enabled=false"
})
@Import(FixedTimeProvider.class)
@Transactional
class PlannedSessionMatchUseCasesIntegrationTest {

    @Autowired
    private MatchImportedRunsUseCase matchImportedRunsUseCase;

    @Autowired
    private CorrectPlannedSessionMatchUseCase correctPlannedSessionMatchUseCase;

    @Autowired
    private UnlinkPlannedSessionMatchUseCase unlinkPlannedSessionMatchUseCase;

    @Autowired
    private GetRunningHistorySnapshotUseCase getRunningHistorySnapshotUseCase;

    @Autowired
    private AdaptiveRunningPlanRepository planRepository;

    @MockitoBean
    private StravaActivitySummaryRepository activityRepository;

    @Test
    void automatic_match_correction_and_unlink_update_persisted_plan_state() {
        AdaptiveRunningPlan plan = planRepository.save(plan());
        Long sessionId = plan.getPlannedSessions().getFirst().getId();
        when(activityRepository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq("runner@sudolife.com"), eq(StravaActivityType.RUN), any(), any()))
                .thenReturn(List.of(activity(10L, "2026-08-03T09:00:00Z", 5200.0, 1850)));

        matchImportedRunsUseCase.execute("runner@sudolife.com");

        assertThat(currentSession().getMatchedActivityId()).isEqualTo(10L);
        when(activityRepository.findByIdAndUserEmail(11L, "runner@sudolife.com"))
                .thenReturn(Optional.of(activity(11L, "2026-08-03T18:00:00Z", 5050.0, 1810)));

        correctPlannedSessionMatchUseCase.execute("runner@sudolife.com",
                new CorrectPlannedSessionMatchCommand(sessionId, 11L));

        assertThat(currentSession().getMatchedActivityId()).isEqualTo(11L);

        unlinkPlannedSessionMatchUseCase.execute("runner@sudolife.com", sessionId);

        assertThat(currentSession().getStatus()).isEqualTo(PlannedSessionStatus.PLANNED);
        assertThat(currentSession().getMatchedActivityId()).isNull();
    }

    @Test
    void unmatched_imported_run_counts_toward_load_without_completing_planned_session() {
        planRepository.save(plan());
        StravaActivitySummary extraRun = activity(12L, "2026-05-10T09:00:00Z", 9000.0, 3200);
        when(activityRepository.findByUserEmailAndActivityTypeAndStartDateBetween(
                eq("runner@sudolife.com"), eq(StravaActivityType.RUN), any(), any()))
                .thenReturn(List.of(extraRun));

        matchImportedRunsUseCase.execute("runner@sudolife.com");
        RunningHistorySnapshotResult history = getRunningHistorySnapshotUseCase.execute("runner@sudolife.com");

        assertThat(currentSession().getStatus()).isEqualTo(PlannedSessionStatus.PLANNED);
        assertThat(history.runningActivityCount()).isEqualTo(1);
        assertThat(history.totalDistanceKilometers()).isEqualTo(9.0);
    }

    private AdaptiveRunningPlanSession currentSession() {
        return planRepository.findLatestByUserEmail("runner@sudolife.com").orElseThrow()
                .getPlannedSessions().getFirst();
    }

    private AdaptiveRunningPlan plan() {
        PlannedSessionResult session = new PlannedSessionResult(1, 1, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse("2026-08-03"), 1800);

        return new AdaptiveRunningPlan(null, "runner@sudolife.com",
                new RunningGoal(10.0, 360, LocalDate.parse("2026-10-01")), "Explanation",
                Instant.parse("2026-07-27T12:00:00Z"), List.of(AdaptiveRunningPlanSession.planned(session)));
    }

    private StravaActivitySummary activity(
            Long id,
            String startDate,
            double distanceMeters,
            int durationSeconds
    ) {
        return new StravaActivitySummary(id, "runner@sudolife.com", 20L, id + 100, StravaActivityType.RUN,
                "Run", "Run", Instant.parse(startDate), distanceMeters, durationSeconds, 3.0, 360.0,
                10.0, 4.0, 150.0, 170.0, 80.0, 200.0, 300.0,
                Instant.parse("2026-08-03T20:00:00Z"));
    }
}
