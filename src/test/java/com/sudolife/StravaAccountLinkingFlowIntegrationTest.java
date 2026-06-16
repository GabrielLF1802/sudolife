package com.sudolife;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaAccountLinkRepository;
import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaAuthorizationStateRepository;
import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAccountLinkEntity;
import com.sudolife.application.service.strava.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.StravaTokenAuthorization;
import com.sudolife.application.service.strava.exception.StravaAuthorizationFailureException;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.RegisterUserCommand;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.CODE;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "strava.frontend-success-redirect-url=https://app.sudolife.com/strava/success",
        "strava.frontend-failure-redirect-url=https://app.sudolife.com/strava/failure"
})
@AutoConfigureMockMvc
@Import({FixedTimeProvider.class, StravaAccountLinkingFlowIntegrationTest.FakeStravaOAuthProviderConfig.class})
@ExtendWith(OutputCaptureExtension.class)
class StravaAccountLinkingFlowIntegrationTest {

    private static final String USER_NAME = "Gabriel";
    private static final String USER_EMAIL = "gabriel-flow@sudolife.com";
    private static final String OTHER_USER_NAME = "Ana";
    private static final String OTHER_USER_EMAIL = "ana-flow@sudolife.com";
    private static final String PASSWORD = "plain-password";
    private static final String ROTATED_ACCESS_TOKEN = "rotated-access-token";
    private static final String ROTATED_REFRESH_TOKEN = "rotated-refresh-token";
    private static final String TOKEN_EXCHANGE_FAILURE_CODE = "exchange-fails";
    private static final String SUCCESS_REDIRECT_URL = "https://app.sudolife.com/strava/success?outcome=success";
    private static final String FAILURE_REDIRECT_URL = "https://app.sudolife.com/strava/failure";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringDataStravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private SpringDataStravaAuthorizationStateRepository authorizationStateRepository;

    @Autowired
    private FakeStravaOAuthProvider oAuthProvider;

    @BeforeEach
    void setUp() {
        authorizationStateRepository.deleteAll();
        accountLinkRepository.deleteAll();
        jdbcTemplate.update("delete from users");
        oAuthProvider.reset();
    }

    @Test
    void register_login_start_callback_and_status_linked_without_token_leakage(CapturedOutput output) throws Exception {
        register(USER_NAME, USER_EMAIL);
        String token = login(USER_EMAIL);

        String state = startLinking(token, ACCESS_TOKEN, REFRESH_TOKEN);
        String redirect = callback(state, CODE, SCOPE);
        String status = getStatus(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.athleteId").value(ATHLETE_ID))
                .andExpect(content().string(not(containsString(ACCESS_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(redirect).isEqualTo(SUCCESS_REDIRECT_URL);
        assertThat(status).doesNotContain(ACCESS_TOKEN, REFRESH_TOKEN);
        assertSafeObservability(output, ACCESS_TOKEN, REFRESH_TOKEN, CODE);
        assertThat(output).contains("Strava account linking started for userEmail=" + USER_EMAIL);
        assertThat(output).contains("Strava account linking completed for userEmail=" + USER_EMAIL);
        assertThat(output).contains("athleteId=" + ATHLETE_ID);
    }

    @Test
    void reconnect_same_user_and_same_athlete_replaces_token_metadata() throws Exception {
        register(USER_NAME, USER_EMAIL);
        String token = login(USER_EMAIL);
        String state = startLinking(token, ACCESS_TOKEN, REFRESH_TOKEN);
        callback(state, CODE, SCOPE);
        Long linkId = activeLink(USER_EMAIL).getId();

        String reconnectState = startLinking(token, ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN);

        callback(reconnectState, CODE, SCOPE, ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN);

        StravaAccountLinkEntity savedLink = activeLink(USER_EMAIL);
        assertThat(savedLink.getId()).isEqualTo(linkId);
        assertThat(savedLink.getAccessToken()).isEqualTo(ROTATED_ACCESS_TOKEN);
        assertThat(savedLink.getRefreshToken()).isEqualTo(ROTATED_REFRESH_TOKEN);
        assertThat(savedLink.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void duplicate_athlete_across_two_users_is_rejected_through_public_flow(CapturedOutput output) throws Exception {
        register(USER_NAME, USER_EMAIL);
        String token = login(USER_EMAIL);
        String state = startLinking(token, ACCESS_TOKEN, REFRESH_TOKEN);
        callback(state, CODE, SCOPE);
        register(OTHER_USER_NAME, OTHER_USER_EMAIL);
        String otherToken = login(OTHER_USER_EMAIL);
        String otherState = startLinking(otherToken, ACCESS_TOKEN, REFRESH_TOKEN);

        String redirect = callback(otherState, CODE, SCOPE);

        assertThat(redirect).isEqualTo(FAILURE_REDIRECT_URL + "?outcome=failure&failureCode=ATHLETE_ALREADY_LINKED");
        assertThat(accountLinkRepository.findByUserEmailAndActiveTrue(OTHER_USER_EMAIL)).isEmpty();
        assertThat(accountLinkRepository.findByAthleteIdAndActiveTrue(ATHLETE_ID)).isPresent();
        assertThat(output).contains("Strava duplicate athlete rejected for athleteId=" + ATHLETE_ID);
        assertSafeObservability(output, ACCESS_TOKEN, REFRESH_TOKEN, CODE);
    }

    @Test
    void unlink_removes_active_status_and_preserves_historical_row(CapturedOutput output) throws Exception {
        register(USER_NAME, USER_EMAIL);
        String token = login(USER_EMAIL);
        String state = startLinking(token, ACCESS_TOKEN, REFRESH_TOKEN);
        callback(state, CODE, SCOPE);

        mockMvc.perform(delete("/api/strava/link").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        getStatus(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(false))
                .andExpect(jsonPath("$.athleteId").doesNotExist())
                .andExpect(content().string(not(containsString(ACCESS_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))));

        List<StravaAccountLinkEntity> links = accountLinkRepository.findByUserEmailOrderByLinkedAtAsc(USER_EMAIL);
        assertThat(accountLinkRepository.findByUserEmailAndActiveTrue(USER_EMAIL)).isEmpty();
        assertThat(links).hasSize(1);
        assertThat(links.getFirst().isActive()).isFalse();
        assertThat(links.getFirst().getAccessToken()).isNull();
        assertThat(links.getFirst().getRefreshToken()).isNull();
        assertThat(links.getFirst().getUnlinkedAt()).isNotNull();
        assertThat(oAuthProvider.deauthorizedAccessTokens()).containsExactly(ACCESS_TOKEN);
        assertThat(output).contains("Strava account unlinked for userEmail=" + USER_EMAIL);
        assertSafeObservability(output, ACCESS_TOKEN, REFRESH_TOKEN, CODE);
    }

    @Test
    void callback_failure_redirects_for_invalid_state_denied_scope_and_token_exchange_without_token_leakage(CapturedOutput output) throws Exception {
        register(USER_NAME, USER_EMAIL);
        String token = login(USER_EMAIL);

        String invalidStateRedirect = callback("invalid-state", CODE, SCOPE);
        String deniedState = startLinking(token, ACCESS_TOKEN, REFRESH_TOKEN);
        String deniedRedirect = callbackDenied(deniedState);
        String insufficientScopeState = startLinking(token, ACCESS_TOKEN, REFRESH_TOKEN);
        String insufficientScopeRedirect = callback(insufficientScopeState, CODE, "profile:read_all");
        String exchangeFailureState = startLinking(token, ACCESS_TOKEN, REFRESH_TOKEN);
        String exchangeFailureRedirect = callback(exchangeFailureState, TOKEN_EXCHANGE_FAILURE_CODE, SCOPE);

        assertThat(invalidStateRedirect).isEqualTo(FAILURE_REDIRECT_URL + "?outcome=failure&failureCode=INVALID_STATE");
        assertThat(deniedRedirect).isEqualTo(FAILURE_REDIRECT_URL + "?outcome=failure&failureCode=AUTHORIZATION_DENIED");
        assertThat(insufficientScopeRedirect).isEqualTo(FAILURE_REDIRECT_URL + "?outcome=failure&failureCode=INSUFFICIENT_SCOPE");
        assertThat(exchangeFailureRedirect).isEqualTo(FAILURE_REDIRECT_URL + "?outcome=failure&failureCode=TOKEN_EXCHANGE_FAILED");
        assertThat(accountLinkRepository.findByUserEmailAndActiveTrue(USER_EMAIL)).isEmpty();
        assertThat(output).contains("Strava account linking failed failureCode=INVALID_STATE");
        assertThat(output).contains("Strava account linking denied for userEmail=" + USER_EMAIL);
        assertThat(output).contains("Strava account linking failed failureCode=INSUFFICIENT_SCOPE");
        assertThat(output).contains("Strava account linking failed failureCode=TOKEN_EXCHANGE_FAILED");
        assertSafeObservability(output, ACCESS_TOKEN, REFRESH_TOKEN, CODE, TOKEN_EXCHANGE_FAILURE_CODE);
    }

    private void register(String name, String email) throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(name, email, PASSWORD);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(email, PASSWORD);

        String response = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        return body.get("token").asText();
    }

    private String startLinking(String token, String accessToken, String refreshToken) throws Exception {
        oAuthProvider.authorize(new StravaTokenAuthorization(ATHLETE_ID, accessToken, refreshToken, EXPIRES_AT, SCOPE));

        String response = mockMvc.perform(post("/api/strava/link").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(content().string(not(containsString(accessToken))))
                .andExpect(content().string(not(containsString(refreshToken))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        return stateFrom(body.get("authorizationUrl").asText());
    }

    private org.springframework.test.web.servlet.ResultActions getStatus(String token) throws Exception {
        return mockMvc.perform(get("/api/strava/status").header("Authorization", "Bearer " + token));
    }

    private String callback(String state, String code, String scope) throws Exception {
        return callback(state, code, scope, ACCESS_TOKEN, REFRESH_TOKEN);
    }

    private String callback(String state, String code, String scope, String accessToken, String refreshToken) throws Exception {
        return mockMvc.perform(get("/api/strava/callback")
                        .param("state", state)
                        .param("code", code)
                        .param("scope", scope))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", not(containsString(code))))
                .andExpect(header().string("Location", not(containsString(accessToken))))
                .andExpect(header().string("Location", not(containsString(refreshToken))))
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
    }

    private String callbackDenied(String state) throws Exception {
        return mockMvc.perform(get("/api/strava/callback")
                        .param("state", state)
                        .param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", not(containsString(CODE))))
                .andExpect(header().string("Location", not(containsString(ACCESS_TOKEN))))
                .andExpect(header().string("Location", not(containsString(REFRESH_TOKEN))))
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
    }

    private StravaAccountLinkEntity activeLink(String email) {
        return accountLinkRepository.findByUserEmailAndActiveTrue(email).orElseThrow();
    }

    private String stateFrom(String authorizationUrl) {
        return UriComponentsBuilder.fromUri(URI.create(authorizationUrl))
                .build()
                .getQueryParams()
                .getFirst("state");
    }

    private void assertSafeObservability(CapturedOutput output, String... sensitiveValues) {
        for (String sensitiveValue : sensitiveValues) {
            assertThat(output).doesNotContain(sensitiveValue);
        }
    }

    @TestConfiguration
    static class FakeStravaOAuthProviderConfig {

        @Bean
        @Primary
        FakeStravaOAuthProvider fakeStravaOAuthProvider() {
            return new FakeStravaOAuthProvider();
        }
    }

    static class FakeStravaOAuthProvider implements StravaOAuthProvider {

        private StravaTokenAuthorization tokenAuthorization;
        private final List<String> deauthorizedAccessTokens = new java.util.ArrayList<>();

        @Override
        public String buildAuthorizationUrl(StravaAuthorizationRequest request) {
            return UriComponentsBuilder.fromUriString("https://fake.strava.test/oauth/authorize")
                    .queryParam("client_id", "fake-client-id")
                    .queryParam("redirect_uri", "https://api.sudolife.test/api/strava/callback")
                    .queryParam("response_type", "code")
                    .queryParam("approval_prompt", "auto")
                    .queryParam("scope", request.scope())
                    .queryParam("state", request.state())
                    .build()
                    .toUriString();
        }

        @Override
        public StravaTokenAuthorization exchangeAuthorizationCode(String code) {
            if (TOKEN_EXCHANGE_FAILURE_CODE.equals(code)) {
                throw new StravaAuthorizationFailureException();
            }

            return tokenAuthorization;
        }

        @Override
        public StravaTokenAuthorization refresh(String refreshToken) {
            return new StravaTokenAuthorization(ATHLETE_ID, ACCESS_TOKEN, refreshToken,
                    Instant.parse("2026-05-11T18:30:00Z"), SCOPE);
        }

        @Override
        public void deauthorize(String accessToken) {
            deauthorizedAccessTokens.add(accessToken);
        }

        void authorize(StravaTokenAuthorization tokenAuthorization) {
            this.tokenAuthorization = tokenAuthorization;
        }

        void reset() {
            tokenAuthorization = new StravaTokenAuthorization(ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT,
                    SCOPE);
            deauthorizedAccessTokens.clear();
        }

        List<String> deauthorizedAccessTokens() {
            return List.copyOf(deauthorizedAccessTokens);
        }
    }
}
