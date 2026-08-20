package com.sudolife;

import com.sudolife.application.service.strava.linking.StravaCallbackResult;
import com.sudolife.application.service.strava.ports.provided.CompleteStravaAccountLinkingUseCase;
import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.RegisterUserCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.sudolife.helper.StravaTestHelper.CODE;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.UserTestHelper.NAME;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "rate-limit-test"})
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false",
        "adaptive-coaching.missed-session-scheduling-enabled=false",
        "strava.summary-sync.scheduling-enabled=false",
        "strava.stream-sync.scheduling-enabled=false",
        "strava.frontend-success-redirect-url=https://app.sudolife.com/strava/success",
        "strava.frontend-failure-redirect-url=https://app.sudolife.com/strava/failure",
        "api.rate-limit.login-ip.enabled=false",
        "api.rate-limit.login-email.enabled=false",
        "api.rate-limit.login-email-origin.enabled=false",
        "api.rate-limit.registration-origin.enabled=false",
        "api.rate-limit.registration-email.enabled=false",
        "api.rate-limit.generic-api.capacity=1",
        "api.rate-limit.generic-api.refill-period=PT1H"
})
@AutoConfigureMockMvc
@Import(GenericApiRateLimitIntegrationTest.GenericApiController.class)
class GenericApiRateLimitIntegrationTest {

    private static final String FIRST_USER_EMAIL = "generic-api-first@sudolife.com";
    private static final String SECOND_USER_EMAIL = "generic-api-second@sudolife.com";
    private static final String EXCLUDED_USER_EMAIL = "generic-api-excluded@sudolife.com";
    private static final String EXCLUDED_SECOND_USER_EMAIL = "generic-api-excluded-second@sudolife.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CompleteStravaAccountLinkingUseCase completeStravaAccountLinkingUseCase;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from users");
    }

    @Test
    void authenticated_requests_are_limited_by_authenticated_user() throws Exception {
        String firstUserToken = tokenFor(FIRST_USER_EMAIL);
        String secondUserToken = tokenFor(SECOND_USER_EMAIL);
        String origin = "203.0.113.60";

        protectedApi(firstUserToken, origin).andExpect(status().isOk());
        protectedApi(secondUserToken, origin).andExpect(status().isOk());

        protectedApi(firstUserToken, origin)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("GENERIC_API_RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("API rate limit exceeded"));
    }

    @Test
    void unauthenticated_requests_are_limited_by_origin() throws Exception {
        String firstOrigin = "203.0.113.61";
        String secondOrigin = "203.0.113.62";

        assertRejectedWithoutToken(firstOrigin);
        assertRejectedWithoutToken(secondOrigin);

        protectedApiWithoutToken(firstOrigin)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("GENERIC_API_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void login_and_register_requests_do_not_consume_generic_api_limit() throws Exception {
        register(EXCLUDED_USER_EMAIL, "203.0.113.63").andExpect(status().isCreated());
        String token = login(EXCLUDED_USER_EMAIL, "203.0.113.63");
        login(EXCLUDED_USER_EMAIL, "203.0.113.63");

        register(EXCLUDED_SECOND_USER_EMAIL, "203.0.113.63").andExpect(status().isCreated());
        protectedApi(token, "203.0.113.63").andExpect(status().isOk());

        protectedApi(token, "203.0.113.63")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("GENERIC_API_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void strava_callback_requests_do_not_consume_generic_api_limit() throws Exception {
        when(completeStravaAccountLinkingUseCase.execute(any())).thenReturn(new StravaCallbackResult(false,
                "AUTHORIZATION_DENIED"));
        String origin = "203.0.113.64";

        stravaCallback(origin).andExpect(status().isFound());
        stravaCallback(origin).andExpect(status().isFound());

        assertRejectedWithoutToken(origin);
    }

    @Test
    void health_requests_do_not_consume_generic_api_limit() throws Exception {
        String token = tokenFor("generic-api-health@sudolife.com");
        String origin = "203.0.113.65";

        mockMvc.perform(get("/actuator/health").header("X-Forwarded-For", origin))
                .andExpect(status().isOk());
        protectedApi(token, origin).andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health/readiness").header("X-Forwarded-For", origin))
                .andExpect(status().isOk());
    }

    private void assertRejectedWithoutToken(String origin) throws Exception {
        int responseStatus = protectedApiWithoutToken(origin)
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(responseStatus).isIn(401, 403);
    }

    private String tokenFor(String email) throws Exception {
        register(email, "203.0.113.66").andExpect(status().isCreated());

        return login(email, "203.0.113.66");
    }

    private ResultActions register(String email, String origin) throws Exception {
        RegisterUserCommand command = new RegisterUserCommand(NAME, email, PASSWORD);

        return mockMvc.perform(post("/api/users/register")
                .header("X-Forwarded-For", origin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));
    }

    private String login(String email, String origin) throws Exception {
        AuthenticateUserCommand command = new AuthenticateUserCommand(email, PASSWORD);

        String response = mockMvc.perform(post("/api/users/login")
                        .header("X-Forwarded-For", origin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        return body.get("token").asText();
    }

    private ResultActions protectedApi(String token, String origin) throws Exception {
        return mockMvc.perform(get("/api/generic-rate-limit")
                .header("Authorization", "Bearer " + token)
                .header("X-Forwarded-For", origin));
    }

    private ResultActions protectedApiWithoutToken(String origin) throws Exception {
        return mockMvc.perform(get("/api/generic-rate-limit")
                .header("X-Forwarded-For", origin));
    }

    private ResultActions stravaCallback(String origin) throws Exception {
        return mockMvc.perform(get("/api/strava/callback")
                .header("X-Forwarded-For", origin)
                .param("state", STATE)
                .param("code", CODE)
                .param("scope", SCOPE));
    }

    @RestController
    @RequestMapping("/api/generic-rate-limit")
    static class GenericApiController {

        @GetMapping
        String genericApiResource() {
            return "ok";
        }
    }
}
