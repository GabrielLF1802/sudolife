package com.sudolife;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false",
        "adaptive-coaching.missed-session-scheduling-enabled=false",
        "strava.summary-sync.scheduling-enabled=false",
        "strava.stream-sync.scheduling-enabled=false"
})
@AutoConfigureMockMvc
class ActuatorHealthEndpointsIntegrationTest {

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
    void liveness_endpoint_is_available() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void readiness_endpoint_is_available_when_database_is_available() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void actuator_web_exposure_is_limited_to_health() {
        String includedEndpoints = environment.getRequiredProperty("management.endpoints.web.exposure.include");

        assertThat(includedEndpoints).isEqualTo("health");
    }

    @Test
    void readiness_includes_database_health() {
        String readinessContributors = environment.getRequiredProperty("management.endpoint.health.group.readiness.include");

        assertThat(readinessContributors).isEqualTo("readinessState,db");
    }
}
