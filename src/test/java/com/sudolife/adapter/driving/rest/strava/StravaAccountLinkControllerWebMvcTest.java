package com.sudolife.adapter.driving.rest.strava;

import com.sudolife.adapter.driving.rest.RestExceptionHandler;
import com.sudolife.application.service.strava.CompleteStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.GetStravaActivityDetailCommand;
import com.sudolife.application.service.strava.GetStravaAccountLinkStatusCommand;
import com.sudolife.application.service.strava.ListStravaActivitiesCommand;
import com.sudolife.application.service.strava.RequestStravaActivitySyncCommand;
import com.sudolife.application.service.strava.StravaActivityDetailEnrichmentStatus;
import com.sudolife.application.service.strava.StravaActivityDetailResult;
import com.sudolife.application.service.strava.StravaActivityListItemResult;
import com.sudolife.application.service.strava.StravaActivityListResult;
import com.sudolife.application.service.strava.StravaActivityStreamStatus;
import com.sudolife.application.service.strava.StartStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.StravaActivitySyncFailureReason;
import com.sudolife.application.service.strava.StravaActivitySyncResult;
import com.sudolife.application.service.strava.StravaActivitySyncStatus;
import com.sudolife.application.service.strava.StravaAuthorizationUrlResult;
import com.sudolife.application.service.strava.StravaCallbackResult;
import com.sudolife.application.service.strava.StravaLinkStatusResult;
import com.sudolife.application.service.strava.StravaPerformanceDataStatus;
import com.sudolife.application.service.strava.StravaPermissionState;
import com.sudolife.application.service.strava.StravaSummaryStatus;
import com.sudolife.application.service.strava.UnlinkStravaAccountCommand;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
import com.sudolife.application.service.strava.ports.provided.CompleteStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.provided.GetStravaActivityDetailUseCase;
import com.sudolife.application.service.strava.ports.provided.GetStravaAccountLinkStatusUseCase;
import com.sudolife.application.service.strava.ports.provided.ListStravaActivitiesUseCase;
import com.sudolife.application.service.strava.ports.provided.RequestStravaActivitySyncUseCase;
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

import java.util.List;

import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.application.model.strava.StravaActivityType.RUN;
import static com.sudolife.helper.StravaTestHelper.ACTIVITY_START_DATE;
import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.CODE;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
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
    private RequestStravaActivitySyncUseCase requestStravaActivitySyncUseCase;

    @MockitoBean
    private ListStravaActivitiesUseCase listStravaActivitiesUseCase;

    @MockitoBean
    private GetStravaActivityDetailUseCase getStravaActivityDetailUseCase;

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
                .thenReturn(new StravaLinkStatusResult(true, ATHLETE_ID, StravaPermissionState.READY,
                        StravaSummaryStatus.COMPLETED, StravaPerformanceDataStatus.PENDING, NOW, null, 4, 1,
                        StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED));

        mockMvc.perform(get("/api/strava/status").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.athleteId").value(ATHLETE_ID))
                .andExpect(jsonPath("$.permissionState").value("READY"))
                .andExpect(jsonPath("$.activitySummaryStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.performanceDataStatus").value("PENDING"))
                .andExpect(jsonPath("$.lastSummarySyncTime").value("2026-05-11T12:00:00Z"))
                .andExpect(jsonPath("$.lastStreamEnrichmentTime").doesNotExist())
                .andExpect(jsonPath("$.importedActivityCount").value(4))
                .andExpect(jsonPath("$.streamsReadyActivityCount").value(1))
                .andExpect(jsonPath("$.failureReason").value("STRAVA_RATE_LIMITED"))
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
    void sync_returns_manual_activity_sync_result_for_authenticated_user() throws Exception {
        RequestStravaActivitySyncCommand command = new RequestStravaActivitySyncCommand(USER_EMAIL);
        when(requestStravaActivitySyncUseCase.execute(command))
                .thenReturn(new StravaActivitySyncResult(StravaActivitySyncStatus.COMPLETED, null, 3, 12));

        mockMvc.perform(post("/api/strava/sync").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.failureReason").doesNotExist())
                .andExpect(jsonPath("$.importedActivityCount").value(3))
                .andExpect(jsonPath("$.totalActivityCount").value(12))
                .andExpect(content().string(not(containsString(ACCESS_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))));

        verify(requestStravaActivitySyncUseCase).execute(command);
    }

    @Test
    void sync_returns_permission_upgrade_required_for_permission_deficient_link() throws Exception {
        RequestStravaActivitySyncCommand command = new RequestStravaActivitySyncCommand(USER_EMAIL);
        when(requestStravaActivitySyncUseCase.execute(command))
                .thenReturn(new StravaActivitySyncResult(StravaActivitySyncStatus.FAILED,
                        StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED, 0, 4));

        mockMvc.perform(post("/api/strava/sync").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("PERMISSION_UPGRADE_REQUIRED"))
                .andExpect(jsonPath("$.importedActivityCount").value(0))
                .andExpect(jsonPath("$.totalActivityCount").value(4));

        verify(requestStravaActivitySyncUseCase).execute(command);
    }

    @Test
    void sync_rejects_unauthenticated_user() throws Exception {
        mockMvc.perform(post("/api/strava/sync"))
                .andExpect(status().isForbidden());
    }

    @Test
    void activities_returns_paginated_activity_list_for_authenticated_user() throws Exception {
        ListStravaActivitiesCommand command = new ListStravaActivitiesCommand(USER_EMAIL, 1, 2);
        when(listStravaActivitiesUseCase.execute(command))
                .thenReturn(activityListResult());

        mockMvc.perform(get("/api/strava/activities")
                        .param("page", "1")
                        .param("size", "2")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.activities[0].id").value(99))
                .andExpect(jsonPath("$.activities[0].sourceActivityId").value(SOURCE_ACTIVITY_ID))
                .andExpect(jsonPath("$.activities[0].name").value("Morning Run"))
                .andExpect(jsonPath("$.activities[0].sportType").value("RUN"))
                .andExpect(jsonPath("$.activities[0].startDate").value("2026-05-10T09:00:00Z"))
                .andExpect(jsonPath("$.activities[0].distanceMeters").value(5000.0))
                .andExpect(jsonPath("$.activities[0].movingTimeSeconds").value(1500))
                .andExpect(jsonPath("$.activities[0].averageSpeedMetersPerSecond").value(3.33))
                .andExpect(jsonPath("$.activities[0].averagePaceSecondsPerKilometer").value(300.0))
                .andExpect(jsonPath("$.activities[0].streamStatus").value("PENDING"))
                .andExpect(content().string(not(containsString(ACCESS_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))));

        verify(listStravaActivitiesUseCase).execute(command);
    }

    @Test
    void activities_uses_default_pagination_parameters() throws Exception {
        ListStravaActivitiesCommand command = new ListStravaActivitiesCommand(USER_EMAIL, 0, 20);
        when(listStravaActivitiesUseCase.execute(command))
                .thenReturn(new StravaActivityListResult(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/strava/activities").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activities").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(listStravaActivitiesUseCase).execute(command);
    }

    @Test
    void activities_rejects_unauthenticated_user() throws Exception {
        mockMvc.perform(get("/api/strava/activities"))
                .andExpect(status().isForbidden());
    }

    @Test
    void activities_rejects_invalid_pagination() throws Exception {
        mockMvc.perform(get("/api/strava/activities")
                        .param("page", "-1")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Page must be zero or greater"));
    }

    @Test
    void activity_returns_detail_for_authenticated_user_without_stream_samples() throws Exception {
        GetStravaActivityDetailCommand command = new GetStravaActivityDetailCommand(USER_EMAIL, 99L);
        when(getStravaActivityDetailUseCase.execute(command))
                .thenReturn(activityDetailResult());

        mockMvc.perform(get("/api/strava/activities/99").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.sourceActivityId").value(SOURCE_ACTIVITY_ID))
                .andExpect(jsonPath("$.name").value("Morning Run Detail"))
                .andExpect(jsonPath("$.sportType").value("RUN"))
                .andExpect(jsonPath("$.startDate").value("2026-05-10T09:00:00Z"))
                .andExpect(jsonPath("$.distanceMeters").value(5100.0))
                .andExpect(jsonPath("$.movingTimeSeconds").value(1510))
                .andExpect(jsonPath("$.totalElevationGainMeters").value(43.0))
                .andExpect(jsonPath("$.averageSpeedMetersPerSecond").value(3.37))
                .andExpect(jsonPath("$.averagePaceSecondsPerKilometer").value(296.08))
                .andExpect(jsonPath("$.maxSpeedMetersPerSecond").value(5.6))
                .andExpect(jsonPath("$.averageHeartRate").value(151.0))
                .andExpect(jsonPath("$.maxHeartRate").value(181.0))
                .andExpect(jsonPath("$.averageCadence").value(83.0))
                .andExpect(jsonPath("$.averageWatts").value(221.0))
                .andExpect(jsonPath("$.calories").value(351.0))
                .andExpect(jsonPath("$.streamStatus").value("PENDING"))
                .andExpect(jsonPath("$.availableStreamMetricNames").isArray())
                .andExpect(jsonPath("$.enrichmentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.elapsedTimeSeconds").doesNotExist())
                .andExpect(jsonPath("$.streamSamples").doesNotExist())
                .andExpect(content().string(not(containsString(ACCESS_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))));

        verify(getStravaActivityDetailUseCase).execute(command);
    }

    @Test
    void activity_rejects_unauthenticated_user() throws Exception {
        mockMvc.perform(get("/api/strava/activities/99"))
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

    private StravaActivityListResult activityListResult() {
        StravaActivityListItemResult item = new StravaActivityListItemResult(99L, SOURCE_ACTIVITY_ID,
                "Morning Run", RUN, ACTIVITY_START_DATE, 5000.0, 1500, 3.33, 300.0,
                StravaActivityStreamStatus.PENDING);

        return new StravaActivityListResult(List.of(item), 1, 2, 3, 2);
    }

    private StravaActivityDetailResult activityDetailResult() {
        return new StravaActivityDetailResult(99L, SOURCE_ACTIVITY_ID, "Morning Run Detail", RUN,
                ACTIVITY_START_DATE, 5100.0, 1510, 43.0, 3.37, 296.08, 5.6, 151.0, 181.0,
                83.0, 221.0, 351.0, StravaActivityStreamStatus.PENDING, List.of(),
                StravaActivityDetailEnrichmentStatus.COMPLETED);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StravaFrontendRedirectProperties stravaFrontendRedirectProperties() {
            return new StravaFrontendRedirectProperties(FRONTEND_SUCCESS_URL, FRONTEND_FAILURE_URL);
        }
    }
}
