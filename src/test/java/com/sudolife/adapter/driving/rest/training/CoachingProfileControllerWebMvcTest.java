package com.sudolife.adapter.driving.rest.training;

import com.sudolife.application.service.training.CoachingProfileResult;
import com.sudolife.application.service.training.AdaptiveRunningPlanResult;
import com.sudolife.application.service.training.ConservativeRunningPlanClassification;
import com.sudolife.application.service.training.ConservativeRunningPlanReason;
import com.sudolife.application.service.training.ConservativeRunningPlanResult;
import com.sudolife.application.service.training.CurrentAdaptiveRunningPlanResult;
import com.sudolife.application.service.training.AdaptiveRunningPlanSessionResult;
import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.PlannedSessionStatus;
import com.sudolife.application.service.training.PlannedSessionTargetResult;
import com.sudolife.application.service.training.PlannedSessionType;
import com.sudolife.application.service.training.SaveCoachingProfileCommand;
import com.sudolife.application.service.training.RunningHistorySnapshotResult;
import com.sudolife.application.service.training.RunningVolumeTrend;
import com.sudolife.application.service.training.RunningGoalAssessmentReason;
import com.sudolife.application.service.training.RunningGoalAssessmentResult;
import com.sudolife.application.service.training.RunningGoalResult;
import com.sudolife.application.service.training.WeeklyRunningVolumeResult;
import com.sudolife.application.service.training.exception.InvalidCoachingProfileException;
import com.sudolife.application.service.training.ports.provided.GetCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.GetCurrentAdaptiveRunningPlanUseCase;
import com.sudolife.application.service.training.ports.provided.GenerateConservativeRunningPlanUseCase;
import com.sudolife.application.service.training.ports.provided.GenerateAdaptiveRunningPlanUseCase;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.provided.EvaluateRunningGoalUseCase;
import com.sudolife.application.service.training.ports.provided.AdaptNextPlannedSessionUseCase;
import com.sudolife.application.service.training.ports.provided.CorrectPlannedSessionMatchUseCase;
import com.sudolife.application.service.training.ports.provided.UnlinkPlannedSessionMatchUseCase;
import com.sudolife.application.service.training.AdaptNextPlannedSessionCommand;
import com.sudolife.application.service.training.AdaptationTrigger;
import com.sudolife.config.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CoachingProfileController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class CoachingProfileControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetCoachingProfileUseCase getUseCase;

    @MockitoBean
    private SaveCoachingProfileUseCase saveUseCase;

    @MockitoBean
    private GetRunningHistorySnapshotUseCase getRunningHistoryUseCase;

    @MockitoBean
    private GenerateConservativeRunningPlanUseCase generateConservativeRunningPlanUseCase;

    @MockitoBean
    private GenerateAdaptiveRunningPlanUseCase generateAdaptiveRunningPlanUseCase;

    @MockitoBean
    private GetCurrentAdaptiveRunningPlanUseCase getCurrentAdaptiveRunningPlanUseCase;

    @MockitoBean
    private EvaluateRunningGoalUseCase evaluateRunningGoalUseCase;

    @MockitoBean
    private AdaptNextPlannedSessionUseCase adaptNextPlannedSessionUseCase;

    @MockitoBean
    private CorrectPlannedSessionMatchUseCase correctPlannedSessionMatchUseCase;

    @MockitoBean
    private UnlinkPlannedSessionMatchUseCase unlinkPlannedSessionMatchUseCase;

    @Test
    void get_running_goal_assessment_returns_long_term_goal_and_safe_milestone() throws Exception {
        RunningGoalAssessmentResult result = new RunningGoalAssessmentResult(
                false,
                List.of(RunningGoalAssessmentReason.UNREALISTIC_DISTANCE),
                new RunningGoalResult(42.2, 330, LocalDate.parse("2026-10-01")),
                new RunningGoalResult(7.3, 350, LocalDate.parse("2026-08-11"))
        );
        when(evaluateRunningGoalUseCase.execute("user@sudolife.com")).thenReturn(result);

        mockMvc.perform(get("/api/coaching-profiles/running-goal-assessment")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realistic").value(false))
                .andExpect(jsonPath("$.reasons[0]").value("UNREALISTIC_DISTANCE"))
                .andExpect(jsonPath("$.longTermGoal.targetDistanceKilometers").value(42.2))
                .andExpect(jsonPath("$.safeMilestone.targetDistanceKilometers").value(7.3));

        verify(evaluateRunningGoalUseCase).execute("user@sudolife.com");
    }

    @Test
    void post_running_plan_returns_structured_conservative_plan_for_authenticated_user() throws Exception {
        PlannedSessionResult session = new PlannedSessionResult(
                1, 1, PlannedSessionType.EASY_RUN, 3.0, PlannedSessionTargetResult.perceivedEffort(4),
                LocalDate.parse("2026-07-18"));
        ConservativeRunningPlanResult result = new ConservativeRunningPlanResult(
                ConservativeRunningPlanClassification.CONSERVATIVE,
                List.of(ConservativeRunningPlanReason.INSUFFICIENT_HISTORY), 21.1, 4, 2, 5, List.of(session));
        when(generateConservativeRunningPlanUseCase.execute("user@sudolife.com")).thenReturn(result);

        mockMvc.perform(post("/api/coaching-profiles/running-plan")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classification").value("CONSERVATIVE"))
                .andExpect(jsonPath("$.reasons[0]").value("INSUFFICIENT_HISTORY"))
                .andExpect(jsonPath("$.plannedSessions[0].type").value("EASY_RUN"))
                .andExpect(jsonPath("$.plannedSessions[0].scheduledDate").value("2026-07-18"))
                .andExpect(jsonPath("$.plannedSessions[0].target.type").value("PERCEIVED_EFFORT"));

        verify(generateConservativeRunningPlanUseCase).execute("user@sudolife.com");
    }

    @Test
    void post_adaptive_running_plan_returns_accepted_plan_for_authenticated_user() throws Exception {
        PlannedSessionResult session = new PlannedSessionResult(
                1, 1, PlannedSessionType.EASY_RUN, 5.0, PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse("2026-07-21"));
        AdaptiveRunningPlanResult result = new AdaptiveRunningPlanResult(
                new RunningGoalResult(7.3, null, LocalDate.parse("2026-08-11")),
                List.of(session), "Plano validado pelo backend.", true);
        when(generateAdaptiveRunningPlanUseCase.execute("user@sudolife.com")).thenReturn(result);

        mockMvc.perform(post("/api/coaching-profiles/adaptive-running-plan")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safeMilestone.targetDistanceKilometers").value(7.3))
                .andExpect(jsonPath("$.plannedSessions[0].type").value("EASY_RUN"))
                .andExpect(jsonPath("$.adjustedBySafetyValidation").value(true));

        verify(generateAdaptiveRunningPlanUseCase).execute("user@sudolife.com");
    }

    @Test
    void get_adaptive_running_plan_returns_current_and_replaced_sessions() throws Exception {
        PlannedSessionResult original = new PlannedSessionResult(
                1, 1, PlannedSessionType.EASY_RUN, 5.0, PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse("2026-07-21"));
        PlannedSessionResult replacement = new PlannedSessionResult(
                1, 1, PlannedSessionType.RECOVERY, 3.0, PlannedSessionTargetResult.perceivedEffort(1, 3),
                LocalDate.parse("2026-07-21"));
        CurrentAdaptiveRunningPlanResult result = new CurrentAdaptiveRunningPlanResult(
                1L,
                new RunningGoalResult(7.3, null, LocalDate.parse("2026-08-11")),
                "Accepted explanation",
                Instant.parse("2026-07-20T12:00:00Z"),
                List.of(
                        new AdaptiveRunningPlanSessionResult(
                                10L, null, original, PlannedSessionStatus.REPLACED, null),
                        new AdaptiveRunningPlanSessionResult(
                                11L, 10L, replacement, PlannedSessionStatus.PLANNED,
                                com.sudolife.application.service.training.AdaptationTrigger.LOW_READINESS)
                )
        );
        when(getCurrentAdaptiveRunningPlanUseCase.execute("user@sudolife.com")).thenReturn(result);

        mockMvc.perform(get("/api/coaching-profiles/adaptive-running-plan")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value("Accepted explanation"))
                .andExpect(jsonPath("$.plannedSessions[0].status").value("REPLACED"))
                .andExpect(jsonPath("$.plannedSessions[1].originalPlannedSessionId").value(10))
                .andExpect(jsonPath("$.plannedSessions[1].status").value("PLANNED"));

        verify(getCurrentAdaptiveRunningPlanUseCase).execute("user@sudolife.com");
    }

    @Test
    void post_adapt_next_planned_session_passes_the_trigger_for_the_authenticated_user() throws Exception {
        AdaptNextPlannedSessionCommand command = new AdaptNextPlannedSessionCommand(AdaptationTrigger.LOW_READINESS);
        CurrentAdaptiveRunningPlanResult result = new CurrentAdaptiveRunningPlanResult(
                1L,
                new RunningGoalResult(7.3, null, LocalDate.parse("2026-08-11")),
                "Accepted explanation",
                Instant.parse("2026-07-20T12:00:00Z"),
                List.of());
        when(adaptNextPlannedSessionUseCase.execute("user@sudolife.com", command)).thenReturn(result);

        mockMvc.perform(post("/api/coaching-profiles/adaptive-running-plan/adapt")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(adaptNextPlannedSessionUseCase).execute("user@sudolife.com", command);
    }

    @Test
    void put_session_match_corrects_match_for_authenticated_user() throws Exception {
        com.sudolife.application.service.training.CorrectPlannedSessionMatchCommand command =
                new com.sudolife.application.service.training.CorrectPlannedSessionMatchCommand(10L, 20L);
        CurrentAdaptiveRunningPlanResult result = currentPlanWithMatchedActivity(20L);
        when(correctPlannedSessionMatchUseCase.execute("user@sudolife.com", command)).thenReturn(result);

        mockMvc.perform(put("/api/coaching-profiles/adaptive-running-plan/session-match")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedSessions[0].matchedActivityId").value(20));

        verify(correctPlannedSessionMatchUseCase).execute("user@sudolife.com", command);
    }

    @Test
    void delete_session_match_unlinks_match_for_authenticated_user() throws Exception {
        CurrentAdaptiveRunningPlanResult result = currentPlanWithMatchedActivity(null);
        when(unlinkPlannedSessionMatchUseCase.execute("user@sudolife.com", 10L)).thenReturn(result);

        mockMvc.perform(delete("/api/coaching-profiles/adaptive-running-plan/sessions/10/match")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedSessions[0].status").value("PLANNED"))
                .andExpect(jsonPath("$.plannedSessions[0].matchedActivityId").doesNotExist());

        verify(unlinkPlannedSessionMatchUseCase).execute("user@sudolife.com", 10L);
    }

    @Test
    void get_running_history_returns_snapshot_for_authenticated_user() throws Exception {
        when(getRunningHistoryUseCase.execute("user@sudolife.com"))
                .thenReturn(new RunningHistorySnapshotResult(true, 3, 4, 24.5, 7200,
                        Instant.parse("2026-07-08T12:00:00Z"),
                        List.of(new WeeklyRunningVolumeResult(0, 2, 12.5, 3600)),
                        0.33, 8.0, 293.88, RunningVolumeTrend.INCREASING));

        mockMvc.perform(get("/api/coaching-profiles/running-history")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sufficientRunningHistory").value(true))
                .andExpect(jsonPath("$.activeWeeks").value(3))
                .andExpect(jsonPath("$.runningActivityCount").value(4))
                .andExpect(jsonPath("$.weeklyRunningVolumes[0].distanceKilometers").value(12.5))
                .andExpect(jsonPath("$.averageRunsPerWeek").value(0.33))
                .andExpect(jsonPath("$.longestRunKilometers").value(8.0))
                .andExpect(jsonPath("$.representativePaceSecondsPerKilometer").value(293.88))
                .andExpect(jsonPath("$.volumeTrend").value("INCREASING"));

        verify(getRunningHistoryUseCase).execute("user@sudolife.com");
    }

    @Test
    void get_returns_coaching_profiles_for_authenticated_user() throws Exception {
        when(getUseCase.execute("user@sudolife.com")).thenReturn(result("LOW", true));

        mockMvc.perform(get("/api/coaching-profiles").principal(authenticated("user@sudolife.com", null,
                        java.util.List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDistanceKilometers").value(10.0))
                .andExpect(jsonPath("$.targetPaceSecondsPerKilometer").value(330))
                .andExpect(jsonPath("$.targetDate").value("2026-05-12"))
                .andExpect(jsonPath("$.readiness").value("LOW"))
                .andExpect(jsonPath("$.injuryConcern").value(true))
                .andExpect(jsonPath("$.configured").value(true));

        verify(getUseCase).execute("user@sudolife.com");
    }

    @Test
    void put_saves_coaching_profiles_for_authenticated_user() throws Exception {
        SaveCoachingProfileCommand command = command("LOW", true);
        when(saveUseCase.execute("user@sudolife.com", command)).thenReturn(result("LOW", true));

        mockMvc.perform(put("/api/coaching-profiles")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDistanceKilometers").value(10.0))
                .andExpect(jsonPath("$.readiness").value("LOW"))
                .andExpect(jsonPath("$.injuryConcern").value(true));

        verify(saveUseCase).execute("user@sudolife.com", command);
    }

    @Test
    void put_returns_bad_request_when_coaching_profiles_are_invalid() throws Exception {
        SaveCoachingProfileCommand command = command("UNSUPPORTED", false);
        when(saveUseCase.execute("user@sudolife.com", command))
                .thenThrow(new InvalidCoachingProfileException("Readiness is unsupported"));

        mockMvc.perform(put("/api/coaching-profiles")
                        .principal(authenticated("user@sudolife.com", null, java.util.List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COACHING_PROFILE"))
                .andExpect(jsonPath("$.message").value("Readiness is unsupported"));
    }

    private SaveCoachingProfileCommand command(String readiness, boolean injuryConcern) {
        return new SaveCoachingProfileCommand(10.0, 330, LocalDate.parse("2026-05-12"), readiness, injuryConcern);
    }

    private CoachingProfileResult result(String readiness, boolean injuryConcern) {
        return new CoachingProfileResult(10.0, 330, LocalDate.parse("2026-05-12"), readiness, injuryConcern,
                List.of("TUESDAY", "SATURDAY"), true);
    }

    private CurrentAdaptiveRunningPlanResult currentPlanWithMatchedActivity(Long activityId) {
        PlannedSessionResult session = new PlannedSessionResult(
                1, 1, PlannedSessionType.EASY_RUN, 5.0, PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse("2026-08-03"), 1800);

        return new CurrentAdaptiveRunningPlanResult(
                1L,
                new RunningGoalResult(10.0, 360, LocalDate.parse("2026-10-01")),
                "Explanation",
                Instant.parse("2026-07-27T12:00:00Z"),
                List.of(new AdaptiveRunningPlanSessionResult(10L, null, session,
                        activityId == null ? PlannedSessionStatus.PLANNED : PlannedSessionStatus.COMPLETED,
                        null, activityId)));
    }
}
