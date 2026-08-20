package com.sudolife;

import com.sudolife.application.service.user.AuthenticateUserCommand;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "rate-limit-test"})
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "strava.summary-sync.scheduling-enabled=false",
        "api.rate-limit.login-ip.capacity=5",
        "api.rate-limit.login-ip.refill-period=PT1H",
        "api.rate-limit.login-email.capacity=3",
        "api.rate-limit.login-email.refill-period=PT1H",
        "api.rate-limit.login-email-origin.capacity=2",
        "api.rate-limit.login-email-origin.refill-period=PT1H",
        "api.rate-limit.registration-origin.enabled=false",
        "api.rate-limit.registration-email.enabled=false",
        "api.rate-limit.generic-api.enabled=false"
})
@AutoConfigureMockMvc
class AuthenticationRateLimitIntegrationTest {

    private static final String WRONG_PASSWORD = "wrong-password";

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
    void login_allows_valid_credentials_within_limits() throws Exception {
        String email = "login-limit-allowed@sudolife.com";
        registerUser(email);

        login(email, PASSWORD, "203.0.113.10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_returns_unauthorized_and_consumes_failed_attempt_limits_for_invalid_credentials() throws Exception {
        String email = "login-limit-invalid@sudolife.com";
        registerUser(email);

        login(email, WRONG_PASSWORD, "203.0.113.11")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        login(email, WRONG_PASSWORD, "203.0.113.11")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_blocks_by_normalized_email_after_failed_attempts_across_origins() throws Exception {
        String email = "login-limit-email@sudolife.com";
        registerUser(email);
        login(" Login-Limit-Email@Sudolife.com ", WRONG_PASSWORD, "203.0.113.12").andExpect(status().isUnauthorized());
        login("login-limit-email@sudolife.com", WRONG_PASSWORD, "203.0.113.13").andExpect(status().isUnauthorized());
        login("LOGIN-LIMIT-EMAIL@SUDOLIFE.COM", WRONG_PASSWORD, "203.0.113.14").andExpect(status().isUnauthorized());

        login(email, WRONG_PASSWORD, "203.0.113.15")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void login_blocks_by_normalized_email_and_origin_after_failed_attempts_from_same_origin() throws Exception {
        String email = "login-limit-origin@sudolife.com";
        registerUser(email);
        login(email, WRONG_PASSWORD, "203.0.113.16").andExpect(status().isUnauthorized());
        login(email, WRONG_PASSWORD, "203.0.113.16").andExpect(status().isUnauthorized());

        login(email, WRONG_PASSWORD, "203.0.113.16")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void login_blocks_by_pre_authentication_origin_before_authentication_work() throws Exception {
        String origin = "203.0.113.17";
        login("origin-1@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());
        login("origin-2@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());
        login("origin-3@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());
        login("origin-4@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());
        login("origin-5@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());

        login("origin-6@sudolife.com", WRONG_PASSWORD, origin)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void successful_login_clears_failed_attempt_limits_without_clearing_pre_authentication_origin_limit() throws Exception {
        String email = "login-limit-clearing@sudolife.com";
        registerUser(email);
        login(email, WRONG_PASSWORD, "203.0.113.18").andExpect(status().isUnauthorized());
        login(email, PASSWORD, "203.0.113.18").andExpect(status().isOk());
        login(email, WRONG_PASSWORD, "203.0.113.18").andExpect(status().isUnauthorized());
        login(email, WRONG_PASSWORD, "203.0.113.18").andExpect(status().isUnauthorized());
        login("post-success-origin-1@sudolife.com", WRONG_PASSWORD, "203.0.113.18").andExpect(status().isUnauthorized());

        login("post-success-origin-2@sudolife.com", WRONG_PASSWORD, "203.0.113.18")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void blocked_login_response_does_not_reveal_account_existence() throws Exception {
        String email = "login-limit-non-leakage@sudolife.com";
        registerUser(email);
        String origin = "203.0.113.19";
        login("unknown-1@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());
        login("unknown-2@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());
        login("unknown-3@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());
        login("unknown-4@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());
        login("unknown-5@sudolife.com", WRONG_PASSWORD, origin).andExpect(status().isUnauthorized());

        String existingUserResponse = blockedLoginResponse(email, origin);
        String unknownUserResponse = blockedLoginResponse("missing-account@sudolife.com", origin);

        assertThat(existingUserResponse).isEqualTo(unknownUserResponse);
    }

    private void registerUser(String email) throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, email, PASSWORD);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated());
    }

    private ResultActions login(String email, String password, String origin) throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(email, password);

        return mockMvc.perform(post("/api/users/login")
                .header("X-Forwarded-For", origin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));
    }

    private String blockedLoginResponse(String email, String origin) throws Exception {
        return login(email, WRONG_PASSWORD, origin)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMIT_EXCEEDED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
