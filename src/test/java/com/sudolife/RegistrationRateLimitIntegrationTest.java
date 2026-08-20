package com.sudolife;

import com.sudolife.application.service.user.RegisterUserCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static com.sudolife.helper.UserTestHelper.NAME;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "rate-limit-test"})
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "strava.summary-sync.scheduling-enabled=false",
        "api.rate-limit.registration-origin.capacity=2",
        "api.rate-limit.registration-origin.refill-period=PT1H",
        "api.rate-limit.registration-email.capacity=2",
        "api.rate-limit.registration-email.refill-period=PT1H"
})
@AutoConfigureMockMvc
class RegistrationRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from users");
    }

    @Test
    void registration_allows_valid_request_within_limits() throws Exception {
        register("registration-allowed@sudolife.com", "203.0.113.40")
                .andExpect(status().isCreated());
    }

    @Test
    void registration_blocks_by_origin_after_allowed_attempts() throws Exception {
        String origin = "203.0.113.41";
        register("registration-origin-1@sudolife.com", origin).andExpect(status().isCreated());
        register("registration-origin-2@sudolife.com", origin).andExpect(status().isCreated());

        register("registration-origin-3@sudolife.com", origin)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("REGISTER_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void registration_blocks_by_normalized_email_after_allowed_attempts() throws Exception {
        register("Registration-Email@Sudolife.com", "203.0.113.42").andExpect(status().isCreated());
        register("registration-email@sudolife.com", "203.0.113.43").andExpect(status().isCreated());

        register("REGISTRATION-EMAIL@SUDOLIFE.COM", "203.0.113.44")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("REGISTER_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void registration_keeps_existing_conflict_behavior_within_limits() throws Exception {
        String email = "registration-conflict@sudolife.com";
        register(email, "203.0.113.45").andExpect(status().isCreated());

        register(email, "203.0.113.45")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    private ResultActions register(String email, String origin) throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, email, PASSWORD);

        return mockMvc.perform(post("/api/users/register")
                .header("X-Forwarded-For", origin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));
    }
}
