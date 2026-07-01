package com.sudolife.adapter.driven.api.strava;

import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.model.strava.StravaActivityDetailImport;
import com.sudolife.application.model.strava.StravaActivityStreamImport;
import com.sudolife.application.service.strava.StravaActivitySummaryImport;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityUnauthorizedException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.REDIRECT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StravaActivityAdapterIntegrationTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String AUTHORIZATION_URL = "https://www.strava.com/oauth/authorize";
    private static final String TOKEN_URL = "https://www.strava.com/api/v3/oauth/token";
    private static final String DEAUTHORIZATION_URL = "https://www.strava.com/oauth/deauthorize";
    private static final String ACTIVITIES_PATH = "/api/v3/athlete/activities";
    private static final String ACTIVITY_DETAIL_PATH = "/api/v3/activities/{activityId}";
    private static final String ACTIVITY_STREAMS_PATH = "/api/v3/activities/{activityId}/streams";
    private static final String ACTIVITY_DETAIL_CONTEXT = "/api/v3/activities";
    private static final Instant AFTER = Instant.parse("2025-10-11T12:00:00Z");
    private static final Instant BEFORE = Instant.parse("2026-05-11T12:00:00Z");

    private HttpServer server;
    private CapturedRequest capturedRequest;
    private int statusCode;
    private String responseBody;
    private StravaActivityAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        statusCode = 200;
        responseBody = activityResponse();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(ACTIVITIES_PATH, this::handleActivities);
        server.createContext(ACTIVITY_DETAIL_CONTEXT, this::handleActivities);
        server.start();
        adapter = new StravaActivityAdapter(stravaApiProperties());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetch_activity_summaries_requests_initial_window_and_maps_supported_types() {
        List<StravaActivitySummaryImport> summaries = adapter.fetchActivitySummaries(ACCESS_TOKEN, AFTER, BEFORE);

        assertThat(summaries).hasSize(4);
        assertThat(summaries).extracting(StravaActivitySummaryImport::activityType)
                .containsExactly(StravaActivityType.RUN, StravaActivityType.WALK, StravaActivityType.RIDE,
                        StravaActivityType.WEIGHT_TRAINING);
        assertThat(summaries.getFirst().sourceActivityId()).isEqualTo(457L);
        assertThat(summaries.getFirst().rawSportType()).isEqualTo("Run");
        assertThat(summaries.getFirst().startDate()).isEqualTo(Instant.parse("2026-05-10T09:00:00Z"));
        assertThat(capturedRequest.authorization()).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(capturedRequest.query()).contains("after=" + AFTER.getEpochSecond());
        assertThat(capturedRequest.query()).contains("before=" + BEFORE.getEpochSecond());
        assertThat(capturedRequest.query()).contains("page=1");
        assertThat(capturedRequest.query()).contains("per_page=200");
    }

    @Test
    void fetch_activity_summaries_ignores_unsupported_sport_types() {
        responseBody = """
                [
                  {
                    "id": 99,
                    "name": "Tennis",
                    "sport_type": "Tennis",
                    "start_date": "2026-05-10T09:00:00Z"
                  }
                ]
                """;

        List<StravaActivitySummaryImport> summaries = adapter.fetchActivitySummaries(ACCESS_TOKEN, AFTER, BEFORE);

        assertThat(summaries).isEmpty();
    }

    @Test
    void fetch_activity_summaries_translates_rate_limit() {
        statusCode = 429;

        assertThatThrownBy(() -> adapter.fetchActivitySummaries(ACCESS_TOKEN, AFTER, BEFORE))
                .isInstanceOf(StravaActivityRateLimitException.class);
    }

    @Test
    void fetch_activity_summaries_translates_unauthorized_response() {
        statusCode = 401;

        assertThatThrownBy(() -> adapter.fetchActivitySummaries(ACCESS_TOKEN, AFTER, BEFORE))
                .isInstanceOf(StravaActivityUnauthorizedException.class);
    }

    @Test
    void fetch_activity_summaries_translates_unavailable_response() {
        statusCode = 503;

        assertThatThrownBy(() -> adapter.fetchActivitySummaries(ACCESS_TOKEN, AFTER, BEFORE))
                .isInstanceOf(StravaActivityUnavailableException.class);
    }

    @Test
    void fetch_activity_detail_requests_activity_by_source_id_and_maps_detail() {
        responseBody = activityDetailResponse();

        StravaActivityDetailImport detail = adapter.fetchActivityDetail(ACCESS_TOKEN, 457L);

        assertThat(detail.sourceActivityId()).isEqualTo(457L);
        assertThat(detail.name()).isEqualTo("Morning Run Detail");
        assertThat(detail.activityType()).isEqualTo(StravaActivityType.RUN);
        assertThat(detail.totalElevationGainMeters()).isEqualTo(43.0);
        assertThat(detail.averageHeartRate()).isEqualTo(151.0);
        assertThat(capturedRequest.authorization()).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(capturedRequest.path()).isEqualTo("/api/v3/activities/457");
    }

    @Test
    void fetch_activity_detail_translates_unavailable_response() {
        statusCode = 503;
        responseBody = activityDetailResponse();

        assertThatThrownBy(() -> adapter.fetchActivityDetail(ACCESS_TOKEN, 457L))
                .isInstanceOf(StravaActivityUnavailableException.class);
    }

    @Test
    void fetch_activity_streams_requests_low_resolution_selected_metrics_without_route_coordinates() {
        responseBody = activityStreamsResponse();

        StravaActivityStreamImport streamImport = adapter.fetchActivityStreams(ACCESS_TOKEN, 457L);

        assertThat(streamImport.availableMetricNames()).containsExactly("time", "distance", "velocity",
                "heart_rate", "cadence", "watts");
        assertThat(streamImport.streamSamplesJson()).doesNotContain("latlng", "map", "polyline");
        assertThat(capturedRequest.path()).isEqualTo("/api/v3/activities/457/streams");
        assertThat(capturedRequest.query()).contains("resolution=low");
        assertThat(capturedRequest.query()).contains("key_by_type=false");
        assertThat(capturedRequest.query()).contains("keys=time,distance,velocity_smooth,heartrate,cadence,watts");
        assertThat(capturedRequest.query()).doesNotContain("latlng", "altitude", "grade_smooth");
    }

    private void handleActivities(HttpExchange exchange) throws IOException {
        capturedRequest = CapturedRequest.from(exchange);
        byte[] responseBytes = responseBody.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private StravaApiProperties stravaApiProperties() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();

        return new StravaApiProperties(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, AUTHORIZATION_URL, TOKEN_URL,
                DEAUTHORIZATION_URL, baseUrl + ACTIVITIES_PATH, baseUrl + ACTIVITY_DETAIL_PATH,
                baseUrl + ACTIVITY_STREAMS_PATH, Duration.ofSeconds(2), Duration.ofSeconds(5));
    }

    private String activityResponse() {
        return """
                [
                  {
                    "id": 457,
                    "name": "Morning Run",
                    "sport_type": "Run",
                    "start_date": "2026-05-10T09:00:00Z",
                    "distance": 5000.0,
                    "moving_time": 1500,
                    "average_speed": 3.33,
                    "total_elevation_gain": 42.0,
                    "max_speed": 5.5,
                    "average_heartrate": 150.0,
                    "max_heartrate": 180.0,
                    "average_cadence": 82.0,
                    "average_watts": 220.0,
                    "calories": 350.0
                  },
                  {
                    "id": 458,
                    "name": "Walk",
                    "sport_type": "Walk",
                    "start_date": "2026-05-10T10:00:00Z"
                  },
                  {
                    "id": 459,
                    "name": "Ride",
                    "sport_type": "Ride",
                    "start_date": "2026-05-10T11:00:00Z"
                  },
                  {
                    "id": 460,
                    "name": "Lift",
                    "sport_type": "WeightTraining",
                    "start_date": "2026-05-10T12:00:00Z"
                  },
                  {
                    "id": 461,
                    "name": "Tennis",
                    "sport_type": "Tennis",
                    "start_date": "2026-05-10T13:00:00Z"
                  }
                ]
                """;
    }

    private String activityDetailResponse() {
        return """
                {
                  "id": 457,
                  "name": "Morning Run Detail",
                  "sport_type": "Run",
                  "start_date": "2026-05-10T09:00:00Z",
                  "distance": 5100.0,
                  "moving_time": 1510,
                  "average_speed": 3.37,
                  "total_elevation_gain": 43.0,
                  "max_speed": 5.6,
                  "average_heartrate": 151.0,
                  "max_heartrate": 181.0,
                  "average_cadence": 83.0,
                  "average_watts": 221.0,
                  "calories": 351.0
                }
                """;
    }

    private String activityStreamsResponse() {
        return """
                [
                  {"type": "time", "data": [0, 30]},
                  {"type": "distance", "data": [0.0, 100.0]},
                  {"type": "velocity_smooth", "data": [3.1, 3.2]},
                  {"type": "heartrate", "data": [140, 142]},
                  {"type": "cadence", "data": [82, 83]},
                  {"type": "watts", "data": [220, 221]},
                  {"type": "latlng", "data": [[-23.5, -46.6]]}
                ]
                """;
    }

    private record CapturedRequest(String authorization, String query, String path) {

        private static CapturedRequest from(HttpExchange exchange) {
            return new CapturedRequest(exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestURI().getQuery(), exchange.getRequestURI().getPath());
        }
    }
}
