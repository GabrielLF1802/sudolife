package com.sudolife.adapter.driven.api.strava.athlete;

import com.sudolife.adapter.driven.api.strava.StravaApiProperties;
import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.service.strava.exception.StravaAthleteProfileUnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StravaAthleteProfileAdapterIntegrationTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String ATHLETE_ZONES_PATH = "/api/v3/athlete/zones";

    private HttpServer server;
    private int responseStatusCode;
    private String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(ATHLETE_ZONES_PATH, this::handleZones);
        server.start();
        responseStatusCode = 200;
        responseBody = zonesResponse();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetchHeartRateZones_returns_usable_strava_zones() {
        StravaAthleteProfileAdapter adapter = new StravaAthleteProfileAdapter(stravaApiProperties());

        List<TrainingHeartRateZone> zones = adapter.fetchHeartRateZones(ACCESS_TOKEN);

        assertThat(zones).containsExactly(
                new TrainingHeartRateZone(100, 120),
                new TrainingHeartRateZone(121, 140),
                new TrainingHeartRateZone(141, 160),
                new TrainingHeartRateZone(161, 180),
                new TrainingHeartRateZone(181, 200)
        );
    }

    @Test
    void fetchHeartRateZones_throws_when_profile_permission_is_unavailable() {
        responseStatusCode = 403;
        StravaAthleteProfileAdapter adapter = new StravaAthleteProfileAdapter(stravaApiProperties());

        assertThatThrownBy(() -> adapter.fetchHeartRateZones(ACCESS_TOKEN))
                .isInstanceOf(StravaAthleteProfileUnavailableException.class);
    }

    @Test
    void fetchHeartRateZones_throws_when_zones_are_unusable() {
        responseBody = """
                {
                  "heart_rate": {
                    "custom_zones": false,
                    "zones": [
                      { "min": 100, "max": 120 }
                    ]
                  }
                }
                """;
        StravaAthleteProfileAdapter adapter = new StravaAthleteProfileAdapter(stravaApiProperties());

        assertThatThrownBy(() -> adapter.fetchHeartRateZones(ACCESS_TOKEN))
                .isInstanceOf(StravaAthleteProfileUnavailableException.class);
    }

    private void handleZones(HttpExchange exchange) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private StravaApiProperties stravaApiProperties() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();

        return new StravaApiProperties("client-id", "client-secret", "redirect-uri", baseUrl + "/oauth/authorize",
                baseUrl + "/oauth/token", baseUrl + "/oauth/deauthorize", baseUrl + "/api/v3/athlete/activities",
                baseUrl + "/api/v3/activities/{activityId}", baseUrl + "/api/v3/activities/{activityId}/streams",
                baseUrl + ATHLETE_ZONES_PATH, Duration.ofSeconds(2), Duration.ofSeconds(5));
    }

    private String zonesResponse() {
        return """
                {
                  "heart_rate": {
                    "custom_zones": false,
                    "zones": [
                      { "min": 100, "max": 120 },
                      { "min": 121, "max": 140 },
                      { "min": 141, "max": 160 },
                      { "min": 161, "max": 180 },
                      { "min": 181, "max": 200 }
                    ]
                  }
                }
                """;
    }
}
