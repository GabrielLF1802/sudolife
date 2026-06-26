package com.sudolife.adapter.driving.rest.strava;

import com.sudolife.adapter.driving.rest.RestExceptionHandler;
import com.sudolife.application.service.strava.CompleteStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.GetStravaAccountLinkStatusCommand;
import com.sudolife.application.service.strava.StartStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.StravaAuthorizationUrlResult;
import com.sudolife.application.service.strava.StravaCallbackResult;
import com.sudolife.application.service.strava.StravaLinkStatusResult;
import com.sudolife.application.service.strava.StravaPermissionState;
import com.sudolife.application.service.strava.UnlinkStravaAccountCommand;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
import com.sudolife.application.service.strava.ports.provided.CompleteStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.provided.GetStravaAccountLinkStatusUseCase;
import com.sudolife.application.service.strava.ports.provided.StartStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.provided.UnlinkStravaAccountUseCase;
import com.sudolife.application.service.user.ports.required.UserRepository;
import com.sudolife.application.service.user.ports.required.UserToken;
import com.sudolife.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.CODE;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StravaAccountLinkController.class)
@Import({SecurityConfig.class, RestExceptionHandler.class, StravaAccountLinkControllerWebMvcTest.TestConfig.class})
class StravaAccountLinkControllerWebMvcTest {

    private static final String AUTHORIZATION_URL = "https://www.strava.com/oauth/authorize?state=" + STATE;
    private static final String FRONTEND_SUCCESS_URL = "https://app.sudolife.com/strava/success";
    private static final String FRONTEND_FAILURE_URL = "https://app.sudolife.com/strava/failure";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartStravaAccountLinkingUseCase startStravaAccountLinkingUseCase;

    @MockitoBean
    private CompleteStravaAccountLinkingUseCase completeStravaAccountLinkingUseCase;

    @MockitoBean
    private GetStravaAccountLinkStatusUseCase getStravaAccountLinkStatusUseCase;

    @MockitoBean
    private UnlinkStravaAccountUseCase unlinkStravaAccountUseCase;

    @MockitoBean
    private UserToken userToken;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void startLinking_returns_authorization_url_for_authenticated_user() throws Exception {
        StartStravaAccountLinkingCommand command = new StartStravaAccountLinkingCommand(USER_EMAIL);
        when(startStravaAccountLinkingUseCase.execute(command))
                .thenReturn(new StravaAuthorizationUrlResult(AUTHORIZATION_URL));

        mockMvc.perform(post("/api/strava/link").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value(AUTHORIZATION_URL))
                .andExpect(content().string(not(containsString(ACCESS_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))));

        verify(startStravaAccountLinkingUseCase).execute(command);
    }

    @Test
    void startLinking_rejects_unauthenticated_user() throws Exception {
        mockMvc.perform(post("/api/strava/link"))
                .andExpect(status().isForbidden());
    }

    @Test
    void status_returns_link_status_for_authenticated_user() throws Exception {
        GetStravaAccountLinkStatusCommand command = new GetStravaAccountLinkStatusCommand(USER_EMAIL);
        when(getStravaAccountLinkStatusUseCase.execute(command))
                .thenReturn(new StravaLinkStatusResult(true, ATHLETE_ID, StravaPermissionState.READY));

        mockMvc.perform(get("/api/strava/status").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.athleteId").value(ATHLETE_ID))
                .andExpect(jsonPath("$.permissionState").value("READY"))
                .andExpect(content().string(not(containsString(ACCESS_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))));

        verify(getStravaAccountLinkStatusUseCase).execute(command);
    }

    @Test
    void status_rejects_unauthenticated_user() throws Exception {
        mockMvc.perform(get("/api/strava/status"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unlink_calls_use_case_for_authenticated_user() throws Exception {
        UnlinkStravaAccountCommand command = new UnlinkStravaAccountCommand(USER_EMAIL);

        mockMvc.perform(delete("/api/strava/link").with(user(USER_EMAIL)))
                .andExpect(status().isNoContent());

        verify(unlinkStravaAccountUseCase).execute(command);
    }

    @Test
    void unlink_rejects_unauthenticated_user() throws Exception {
        mockMvc.perform(delete("/api/strava/link"))
                .andExpect(status().isForbidden());
    }

    @Test
    void callback_redirects_to_success_url_without_sensitive_values() throws Exception {
        CompleteStravaAccountLinkingCommand command = new CompleteStravaAccountLinkingCommand(STATE, CODE, SCOPE, null);
        when(completeStravaAccountLinkingUseCase.execute(command))
                .thenReturn(new StravaCallbackResult(true, null));

        mockMvc.perform(get("/api/strava/callback")
                        .param("state", STATE)
                        .param("code", CODE)
                        .param("scope", SCOPE))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", FRONTEND_SUCCESS_URL + "?outcome=success"))
                .andExpect(header().string("Location", not(containsString(CODE))))
                .andExpect(header().string("Location", not(containsString(ACCESS_TOKEN))))
                .andExpect(header().string("Location", not(containsString(REFRESH_TOKEN))));

        verify(completeStravaAccountLinkingUseCase).execute(command);
    }

    @Test
    void callback_redirects_to_failure_url_without_sensitive_values() throws Exception {
        CompleteStravaAccountLinkingCommand command = new CompleteStravaAccountLinkingCommand(STATE, null, null,
                "access_denied");
        when(completeStravaAccountLinkingUseCase.execute(command))
                .thenReturn(new StravaCallbackResult(false, "AUTHORIZATION_DENIED"));

        mockMvc.perform(get("/api/strava/callback")
                        .param("state", STATE)
                        .param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        FRONTEND_FAILURE_URL + "?outcome=failure&failureCode=AUTHORIZATION_DENIED"))
                .andExpect(header().string("Location", not(containsString(CODE))))
                .andExpect(header().string("Location", not(containsString(ACCESS_TOKEN))))
                .andExpect(header().string("Location", not(containsString(REFRESH_TOKEN))));

        verify(completeStravaAccountLinkingUseCase).execute(command);
    }

    @Test
    void duplicate_strava_athlete_returns_user_safe_conflict_response() throws Exception {
        StartStravaAccountLinkingCommand command = new StartStravaAccountLinkingCommand(USER_EMAIL);
        doThrow(new DuplicateStravaAthleteOwnershipException())
                .when(startStravaAccountLinkingUseCase)
                .execute(command);

        mockMvc.perform(post("/api/strava/link").with(user(USER_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATHLETE_ALREADY_LINKED"))
                .andExpect(jsonPath("$.message").value("Strava athlete is already linked to another user"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StravaFrontendRedirectProperties stravaFrontendRedirectProperties() {
            return new StravaFrontendRedirectProperties(FRONTEND_SUCCESS_URL, FRONTEND_FAILURE_URL);
        }
    }
}
