package com.sudolife.adapter.driven.api.training;

import com.sudolife.application.service.training.AiRunningPlanProposal;
import com.sudolife.application.service.training.TrainingSnapshot;
import com.sudolife.application.service.training.ports.required.AiRunningPlanProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class HttpAiRunningPlanProvider implements AiRunningPlanProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    @Autowired
    public HttpAiRunningPlanProvider(
            ObjectMapper objectMapper,
            @Value("${ai.running-plan-provider-url}") String providerUrl,
            @Value("${ai.running-plan-provider-model}") String model,
            @Value("${ai.running-plan-provider-connect-timeout}") Duration connectTimeout,
            @Value("${ai.running-plan-provider-read-timeout}") Duration readTimeout
    ) {
        this(RestClient.builder().baseUrl(providerUrl).requestFactory(requestFactory(connectTimeout, readTimeout)).build(),
                objectMapper, model);
    }

    HttpAiRunningPlanProvider(RestClient restClient, ObjectMapper objectMapper, String model) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public AiRunningPlanProposal draft(TrainingSnapshot snapshot) {
        OllamaChatResponse response = restClient.post()
                .uri("/api/chat")
                .body(request(snapshot))
                .retrieve()
                .body(OllamaChatResponse.class);

        if (response == null || response.message() == null || response.message().content() == null) {
            throw new IllegalStateException("Ollama returned an empty running plan proposal");
        }

        try {
            return objectMapper.readValue(response.message().content(), AiRunningPlanProposal.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Ollama returned an invalid running plan proposal", exception);
        }
    }

    private OllamaChatRequest request(TrainingSnapshot snapshot) {
        try {
            return new OllamaChatRequest(
                    model,
                    List.of(new OllamaMessage("system", systemPrompt()),
                            new OllamaMessage("user", objectMapper.writeValueAsString(snapshot))),
                    false,
                    proposalSchema(),
                    Map.of("temperature", 0));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize training snapshot", exception);
        }
    }

    private static JdkClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        requestFactory.setReadTimeout(readTimeout);
        return requestFactory;
    }

    private String systemPrompt() {
        return "You draft explanations for four-week running plans. Return only JSON matching the provided schema. "
                + "Copy every planned session exactly as supplied in the user context. "
                + "Write the explanation in Brazilian Portuguese.";
    }

    private Map<String, Object> proposalSchema() {
        Map<String, Object> target = Map.of(
                "type", "object",
                "properties", Map.of(
                        "type", Map.of("type", "string", "enum", List.of("HEART_RATE", "PERCEIVED_EFFORT")),
                        "minimumHeartRate", Map.of("type", List.of("integer", "null")),
                        "maximumHeartRate", Map.of("type", List.of("integer", "null")),
                        "minimumPerceivedEffort", Map.of("type", List.of("integer", "null")),
                        "maximumPerceivedEffort", Map.of("type", List.of("integer", "null"))),
                "required", List.of("type", "minimumHeartRate", "maximumHeartRate", "minimumPerceivedEffort",
                        "maximumPerceivedEffort"));
        Map<String, Object> session = Map.of(
                "type", "object",
                "properties", Map.of(
                        "weekNumber", Map.of("type", "integer"),
                        "sessionNumber", Map.of("type", "integer"),
                        "type", Map.of("type", "string", "enum", List.of("EASY_RUN", "LONG_RUN", "RECOVERY")),
                        "distanceKilometers", Map.of("type", "number"),
                        "target", target,
                        "scheduledDate", Map.of("type", "string")),
                "required", List.of("weekNumber", "sessionNumber", "type", "distanceKilometers", "target",
                        "scheduledDate"));
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "plannedSessions", Map.of("type", "array", "items", session),
                        "explanation", Map.of("type", "string")),
                "required", List.of("plannedSessions", "explanation"));
    }

    private record OllamaChatRequest(
            String model,
            List<OllamaMessage> messages,
            boolean stream,
            Map<String, Object> format,
            Map<String, Integer> options
    ) {
    }

    private record OllamaChatResponse(OllamaMessage message) {
    }

    private record OllamaMessage(String role, String content) {
    }
}
