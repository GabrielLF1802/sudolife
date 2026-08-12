package com.sudolife;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false",
        "adaptive-coaching.missed-session-scheduling-enabled=false",
        "strava.summary-sync.scheduling-enabled=false",
        "strava.stream-sync.scheduling-enabled=false",
        "DB_URL=jdbc:h2:mem:prod_profile;DB_CLOSE_DELAY=-1",
        "DB_USER=sa",
        "DB_PASSWORD=",
        "API_SECURITY_TOKEN_SECRET=test-only-token-secret-with-at-least-thirty-two-characters",
        "API_SECURITY_TOKEN_ISSUER=sudolife-api",
        "CORS_ALLOWED_ORIGINS=https://app.sudolife.example",
        "STRAVA_CLIENT_ID=123",
        "STRAVA_CLIENT_SECRET=test-only-strava-client-secret",
        "STRAVA_AUTHORIZATION_URL=https://www.strava.com/oauth/authorize",
        "STRAVA_TOKEN_URL=https://www.strava.com/api/v3/oauth/token",
        "STRAVA_DEAUTHORIZATION_URL=https://www.strava.com/oauth/deauthorize",
        "STRAVA_ACTIVITIES_URL=https://www.strava.com/api/v3/athlete/activities",
        "STRAVA_ACTIVITY_DETAIL_URL=https://www.strava.com/api/v3/activities/{activityId}",
        "STRAVA_ACTIVITY_STREAMS_URL=https://www.strava.com/api/v3/activities/{activityId}/streams",
        "STRAVA_ATHLETE_ZONES_URL=https://www.strava.com/api/v3/athlete/zones",
        "STRAVA_REDIRECT_URI=https://api.sudolife.example/api/strava/callback",
        "STRAVA_FRONTEND_SUCCESS_REDIRECT_URL=https://app.sudolife.example/settings/connections/strava/success",
        "STRAVA_FRONTEND_FAILURE_REDIRECT_URL=https://app.sudolife.example/settings/connections/strava/failure",
        "AI_RUNNING_PLAN_PROVIDER_URL=http://ollama-sudolife:11434",
        "AI_RUNNING_PLAN_PROVIDER_MODEL=llama3:8b"
})
@AutoConfigureMockMvc
class ProductionProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void health_endpoint_is_available_without_details() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void swagger_ui_is_not_publicly_available() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isForbidden());
    }

    @Test
    void api_docs_are_not_publicly_available() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cors_allows_configured_production_origin() throws Exception {
        mockMvc.perform(options("/api/users/login")
                        .header(HttpHeaders.ORIGIN, "https://app.sudolife.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.sudolife.example"));
    }

    @Test
    void production_profile_uses_environment_driven_external_settings() {
        assertThat(environment.getRequiredProperty("ai.running-plan-provider-url")).isEqualTo("http://ollama-sudolife:11434");
        assertThat(environment.getRequiredProperty("strava.redirect-uri")).isEqualTo("https://api.sudolife.example/api/strava/callback");
        assertThat(environment.getRequiredProperty("api.cors.allowed-origins")).isEqualTo("https://app.sudolife.example");
        assertThat(environment.getRequiredProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
        assertThat(environment.getRequiredProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(environment.getRequiredProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getRequiredProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }
}
