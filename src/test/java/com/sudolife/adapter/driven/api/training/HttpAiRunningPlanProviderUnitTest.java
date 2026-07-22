package com.sudolife.adapter.driven.api.training;

import com.sudolife.application.service.training.AiRunningPlanProposal;
import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.PlannedSessionTargetResult;
import com.sudolife.application.service.training.PlannedSessionType;
import com.sudolife.application.service.training.RunningGoalResult;
import com.sudolife.application.service.training.RunningHistorySnapshotResult;
import com.sudolife.application.service.training.TrainingSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpAiRunningPlanProviderUnitTest {

    @Test
    void draft_with_structured_ollama_response_returns_running_plan_proposal() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://ollama.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        HttpAiRunningPlanProvider provider = new HttpAiRunningPlanProvider(
                restClientBuilder.build(), new ObjectMapper(), "llama3:8b");
        server.expect(requestTo("http://ollama.test/api/chat"))
                .andExpect(method(POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"llama3:8b\"")))
                .andRespond(withSuccess(ollamaResponse(), MediaType.APPLICATION_JSON));

        AiRunningPlanProposal proposal = provider.draft(snapshot());

        assertThat(proposal.explanation()).isEqualTo("Plano conservador para esta semana.");
        assertThat(proposal.plannedSessions()).containsExactly(session());
        server.verify();
    }

    private TrainingSnapshot snapshot() {
        return new TrainingSnapshot(
                new RunningHistorySnapshotResult(true, 3, 4, 20.0, 7200, null),
                new RunningGoalResult(10.0, null, null),
                List.of(),
                List.of(session()));
    }

    private PlannedSessionResult session() {
        return new PlannedSessionResult(1, 1, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse("2026-07-23"));
    }

    private String ollamaResponse() {
        return """
                {"message":{"role":"assistant","content":"{\\"plannedSessions\\":[{\\"weekNumber\\":1,\\"sessionNumber\\":1,\\"type\\":\\"EASY_RUN\\",\\"distanceKilometers\\":5.0,\\"target\\":{\\"type\\":\\"PERCEIVED_EFFORT\\",\\"minimumHeartRate\\":null,\\"maximumHeartRate\\":null,\\"minimumPerceivedEffort\\":2,\\"maximumPerceivedEffort\\":4},\\"scheduledDate\\":\\"2026-07-23\\"}],\\"explanation\\":\\"Plano conservador para esta semana.\\"}"}}
                """;
    }
}
