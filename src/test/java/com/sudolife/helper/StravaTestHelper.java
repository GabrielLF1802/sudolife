package com.sudolife.helper;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.CompleteStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.GetStravaAccountLinkStatusCommand;
import com.sudolife.application.service.strava.StartStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.StravaTokenAuthorization;
import com.sudolife.application.service.strava.UnlinkStravaAccountCommand;

import java.time.Instant;

public class StravaTestHelper {

    public static final Long LINK_ID = 10L;
    public static final String USER_EMAIL = "gabriel@sudolife.com";
    public static final Long ATHLETE_ID = 9001L;
    public static final String ACCESS_TOKEN = "access-token";
    public static final String REFRESH_TOKEN = "refresh-token";
    public static final String STATE = "state-token";
    public static final String CODE = "authorization-code";
    public static final String SCOPE = "read,activity:read";
    public static final String REDIRECT_URI = "https://sudolife.com/api/strava/callback";
    public static final Instant NOW = Instant.parse("2026-05-11T12:00:00Z");
    public static final Instant EXPIRES_AT = Instant.parse("2026-05-11T18:00:00Z");
    public static final Instant LINKED_AT = Instant.parse("2026-05-11T12:01:00Z");
    public static final Instant UNLINKED_AT = Instant.parse("2026-05-11T12:05:00Z");
    public static final Long SOURCE_ACTIVITY_ID = 457L;
    public static final Instant ACTIVITY_START_DATE = Instant.parse("2026-05-10T09:00:00Z");

    public static StravaAccountLink activeStravaAccountLink() {
        return StravaAccountLink.active(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT,
                LINKED_AT);
    }

    public static StravaAccountLink inactiveStravaAccountLink() {
        return new StravaAccountLink(LINK_ID, USER_EMAIL, ATHLETE_ID, null, null, null, false, LINKED_AT, UNLINKED_AT);
    }

    public static StravaAuthorizationState pendingAuthorizationState() {
        return StravaAuthorizationState.pending(STATE, USER_EMAIL, EXPIRES_AT);
    }

    public static StravaAuthorizationState consumedAuthorizationState() {
        return new StravaAuthorizationState(STATE, USER_EMAIL, EXPIRES_AT, NOW);
    }

    public static StartStravaAccountLinkingCommand startStravaAccountLinkingCommand() {
        return new StartStravaAccountLinkingCommand(USER_EMAIL);
    }

    public static CompleteStravaAccountLinkingCommand completeStravaAccountLinkingCommand() {
        return new CompleteStravaAccountLinkingCommand(STATE, CODE, SCOPE, null);
    }

    public static GetStravaAccountLinkStatusCommand getStravaAccountLinkStatusCommand() {
        return new GetStravaAccountLinkStatusCommand(USER_EMAIL);
    }

    public static UnlinkStravaAccountCommand unlinkStravaAccountCommand() {
        return new UnlinkStravaAccountCommand(USER_EMAIL);
    }

    public static StravaAuthorizationRequest stravaAuthorizationRequest() {
        return new StravaAuthorizationRequest(STATE, REDIRECT_URI, SCOPE);
    }

    public static StravaTokenAuthorization stravaTokenAuthorization() {
        return new StravaTokenAuthorization(ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, SCOPE);
    }

    public static StravaActivitySummary stravaActivitySummary() {
        return StravaActivitySummary.imported(USER_EMAIL, LINK_ID, SOURCE_ACTIVITY_ID, StravaActivityType.RUN, "Run",
                "Morning Run", ACTIVITY_START_DATE, 5000.0, 1500, 3.33, 42.0, 5.5, 150.0, 180.0, 82.0,
                220.0, 350.0, NOW);
    }
}
