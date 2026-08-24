package com.sudolife;

import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.CompletePasswordRecoveryCommand;
import com.sudolife.application.service.user.IssuedPasswordRecoveryToken;
import com.sudolife.application.service.user.RegisterUserCommand;
import com.sudolife.application.service.user.StartPasswordRecoveryCommand;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static com.sudolife.helper.UserTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import({
        PasswordRecoveryFlowIntegrationTest.FixedPasswordRecoveryTokenProviderConfiguration.class,
        com.sudolife.helper.FixedTimeProvider.class
})
class PasswordRecoveryFlowIntegrationTest {

    private static final String RAW_TOKEN = "deterministic-recovery-token";
    private static final String GENERIC_MESSAGE = "Se uma conta existir para esse email, instruções de recuperação de senha serão enviadas.";

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
    void start_creates_hashed_token_for_registered_email_and_replaces_active_tokens() throws Exception {
        registerUser();
        startRecovery(EMAIL);
        Map<String, Object> firstToken = onlyToken();

        startRecovery(EMAIL);

        assertThat(tokenCount()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "select used_at from password_recovery_tokens where id = ?",
                Object.class,
                ((Number) firstToken.get("ID")).longValue()
        )).isNotNull();
        assertThat(onlyActiveToken().get("EXPIRES_AT").toString()).startsWith("2026-05-11T12:30");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from password_recovery_tokens where user_email = ? and used_at is null",
                Integer.class,
                EMAIL
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from password_recovery_tokens where token_hash like ?",
                Integer.class,
                RAW_TOKEN + "%"
        )).isZero();
    }

    @Test
    void start_returns_same_response_without_token_for_unregistered_email() throws Exception {
        startRecovery(EMAIL);

        assertThat(tokenCount()).isZero();
    }

    @Test
    void complete_consumes_token_and_allows_login_with_new_password_only() throws Exception {
        registerUser();
        startRecovery(EMAIL);

        String rawToken = onlyActiveRawToken();

        completeRecovery(rawToken, NEW_PASSWORD);

        assertThat(onlyToken().get("USED_AT")).isNotNull();
        login(EMAIL, PASSWORD, status().isUnauthorized());
        login(EMAIL, NEW_PASSWORD, status().isOk());
    }

    @Test
    void complete_rejects_reused_token_without_changing_password_again() throws Exception {
        registerUser();
        startRecovery(EMAIL);
        String rawToken = onlyActiveRawToken();
        completeRecovery(rawToken, NEW_PASSWORD);

        mockMvc.perform(post("/api/auth/password-recovery/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompletePasswordRecoveryCommand(rawToken, "Valid!Second2")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RECOVERY_TOKEN"));

        login(EMAIL, NEW_PASSWORD, status().isOk());
    }

    @Test
    void complete_rejects_unknown_token_without_changing_password() throws Exception {
        registerUser();

        mockMvc.perform(post("/api/auth/password-recovery/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompletePasswordRecoveryCommand("unknown-token", NEW_PASSWORD)
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RECOVERY_TOKEN"));

        login(EMAIL, PASSWORD, status().isOk());
    }

    @Test
    void complete_returns_password_policy_failures_without_changing_password() throws Exception {
        registerUser();
        startRecovery(EMAIL);
        String rawToken = onlyActiveRawToken();

        mockMvc.perform(post("/api/auth/password-recovery/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompletePasswordRecoveryCommand(rawToken, "weak")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"))
                .andExpect(jsonPath("$.violations").isArray());

        assertThat(onlyActiveToken()).isNotNull();
        login(EMAIL, PASSWORD, status().isOk());
    }

    private void startRecovery(String email) throws Exception {
        StartPasswordRecoveryCommand command = new StartPasswordRecoveryCommand(email);

        mockMvc.perform(post("/api/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    private void completeRecovery(String token, String newPassword) throws Exception {
        CompletePasswordRecoveryCommand command = new CompletePasswordRecoveryCommand(token, newPassword);

        mockMvc.perform(post("/api/auth/password-recovery/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNoContent());
    }

    private void login(String email, String password, ResultMatcher expectedStatus) throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(email, password);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(expectedStatus);
    }

    private void registerUser() throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, PASSWORD);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated());
    }

    private Map<String, Object> onlyToken() {
        return jdbcTemplate.queryForMap("select * from password_recovery_tokens");
    }

    private Map<String, Object> onlyActiveToken() {
        return jdbcTemplate.queryForMap("select * from password_recovery_tokens where used_at is null");
    }

    private String onlyActiveRawToken() {
        String tokenHash = (String) onlyActiveToken().get("TOKEN_HASH");

        return tokenHash.replaceFirst("^hashed-", "");
    }

    private int tokenCount() {
        return jdbcTemplate.queryForObject("select count(*) from password_recovery_tokens", Integer.class);
    }

    @TestConfiguration
    static class FixedPasswordRecoveryTokenProviderConfiguration {

        private final AtomicInteger sequence = new AtomicInteger();

        @Bean
        @Primary
        PasswordRecoveryTokenProvider fixedPasswordRecoveryTokenProvider() {
            return new PasswordRecoveryTokenProvider() {
                @Override
                public IssuedPasswordRecoveryToken provide() {
                    String rawToken = RAW_TOKEN + sequence.incrementAndGet();

                    return new IssuedPasswordRecoveryToken(rawToken, hash(rawToken));
                }

                @Override
                public String hash(String token) {
                    return "hashed-" + token;
                }
            };
        }
    }
}
