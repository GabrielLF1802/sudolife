package com.sudolife;

import com.sudolife.application.service.user.CompletePasswordRecoveryCommand;
import com.sudolife.application.service.user.RegisterUserCommand;
import com.sudolife.application.service.user.StartPasswordRecoveryCommand;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "rate-limit-test"})
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "strava.summary-sync.scheduling-enabled=false",
        "api.rate-limit.login-ip.enabled=false",
        "api.rate-limit.login-email.enabled=false",
        "api.rate-limit.login-email-origin.enabled=false",
        "api.rate-limit.registration-origin.enabled=false",
        "api.rate-limit.registration-email.enabled=false",
        "api.rate-limit.generic-api.enabled=false",
        "api.rate-limit.password-recovery-start-origin.capacity=2",
        "api.rate-limit.password-recovery-start-origin.refill-period=PT1H",
        "api.rate-limit.password-recovery-start-email.capacity=2",
        "api.rate-limit.password-recovery-start-email.refill-period=PT1H",
        "api.rate-limit.password-recovery-complete-origin.capacity=2",
        "api.rate-limit.password-recovery-complete-origin.refill-period=PT1H"
})
@AutoConfigureMockMvc
class PasswordRecoveryRateLimitIntegrationTest {

    private static final String GENERIC_MESSAGE =
            "Se uma conta existir para esse email, instruções de recuperação de senha serão enviadas.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from password_recovery_tokens");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void start_blocks_by_origin_after_allowed_attempts() throws Exception {
        String origin = "203.0.113.70";
        startRecovery("recovery-origin-1@sudolife.com", origin).andExpect(status().isOk());
        startRecovery("recovery-origin-2@sudolife.com", origin).andExpect(status().isOk());

        startRecovery("recovery-origin-3@sudolife.com", origin)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("PASSWORD_RECOVERY_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void start_blocks_by_normalized_email_after_allowed_attempts_across_origins() throws Exception {
        startRecovery(" Recovery-Email@Sudolife.com ", "203.0.113.71").andExpect(status().isOk());
        startRecovery("recovery-email@sudolife.com", "203.0.113.72").andExpect(status().isOk());

        startRecovery("RECOVERY-EMAIL@SUDOLIFE.COM", "203.0.113.73")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("PASSWORD_RECOVERY_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void start_returns_same_external_response_for_registered_and_unregistered_email_before_limit() throws Exception {
        String registeredEmail = "recovery-existing@sudolife.com";
        registerUser(registeredEmail);

        String registeredResponse = startRecoveryResponse(registeredEmail, "203.0.113.74");
        String unregisteredResponse = startRecoveryResponse("recovery-missing@sudolife.com", "203.0.113.75");

        assertThat(registeredResponse).isEqualTo(unregisteredResponse);
    }

    @Test
    void complete_blocks_by_origin_after_allowed_public_attempts() throws Exception {
        String origin = "203.0.113.76";
        completeRecovery("unknown-token-1", origin).andExpect(status().isBadRequest());
        completeRecovery("unknown-token-2", origin).andExpect(status().isBadRequest());

        completeRecovery("unknown-token-3", origin)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("PASSWORD_RECOVERY_RATE_LIMIT_EXCEEDED"));
    }

    private void registerUser(String email) throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, email, PASSWORD);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated());
    }

    private ResultActions startRecovery(String email, String origin) throws Exception {
        StartPasswordRecoveryCommand command = new StartPasswordRecoveryCommand(email);

        return mockMvc.perform(post("/api/auth/password-recovery")
                .header("X-Forwarded-For", origin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));
    }

    private String startRecoveryResponse(String email, String origin) throws Exception {
        return startRecovery(email, origin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private ResultActions completeRecovery(String token, String origin) throws Exception {
        CompletePasswordRecoveryCommand command = new CompletePasswordRecoveryCommand(token, "Strong!Password2");

        return mockMvc.perform(post("/api/auth/password-recovery/complete")
                .header("X-Forwarded-For", origin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));
    }
}
