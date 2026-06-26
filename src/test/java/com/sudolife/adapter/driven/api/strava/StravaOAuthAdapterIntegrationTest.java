package com.sudolife.adapter.driven.api.strava;

import com.sudolife.application.service.strava.StravaTokenAuthorization;
import com.sudolife.application.service.strava.exception.StravaAuthorizationFailureException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.CODE;
import static com.sudolife.helper.StravaTestHelper.REDIRECT_URI;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StravaOAuthAdapterIntegrationTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String AUTHORIZATION_PATH = "/oauth/authorize";
    private static final String TOKEN_PATH = "/api/v3/oauth/token";
    private static final String DEAUTHORIZATION_PATH = "/oauth/deauthorize";
    private static final String ACTIVITIES_PATH = "/api/v3/athlete/activities";
    private static final Long EXPIRES_AT_EPOCH_SECOND = 1773507600L;
    private static final String TOKEN_RESPONSE = """
            {
              "access_token": "access-token",
              "refresh_token": "refresh-token",
              "expires_at": 1773507600,
              "scope": "read,activity:read",
              "athlete": {
                "id": 9001
              }
            }
            """;

    private HttpServer server;
    private CapturedRequest capturedRequest;
    private int tokenStatusCode;
    private int deauthorizationStatusCode;
    private String tokenResponse;
    private StravaOAuthAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        tokenStatusCode = 200;
        deauthorizationStatusCode = 200;
        tokenResponse = TOKEN_RESPONSE;
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(TOKEN_PATH, this::handleToken);
        server.createContext(DEAUTHORIZATION_PATH, this::handleDeauthorization);
        server.start();
        adapter = new StravaOAuthAdapter(stravaApiProperties());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void exchange_authorization_code_posts_form_request_and_maps_response() {
        StravaTokenAuthorization authorization = adapter.exchangeAuthorizationCode(CODE);

        assertThat(authorization.athleteId()).isEqualTo(ATHLETE_ID);
        assertThat(authorization.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(authorization.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(authorization.expiresAt()).isEqualTo(Instant.ofEpochSecond(EXPIRES_AT_EPOCH_SECOND));
        assertThat(authorization.scope()).isEqualTo(SCOPE);
        assertThat(capturedRequest.method()).isEqualTo("POST");
        assertThat(capturedRequest.path()).isEqualTo(TOKEN_PATH);
        assertThat(capturedRequest.form()).containsEntry("client_id", CLIENT_ID);
        assertThat(capturedRequest.form()).containsEntry("client_secret", CLIENT_SECRET);
        assertThat(capturedRequest.form()).containsEntry("grant_type", "authorization_code");
        assertThat(capturedRequest.form()).containsEntry("code", CODE);
    }

    @Test
    void refresh_posts_refresh_form_request_and_maps_response() {
        StravaTokenAuthorization authorization = adapter.refresh(REFRESH_TOKEN);

        assertThat(authorization.athleteId()).isEqualTo(ATHLETE_ID);
        assertThat(authorization.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(authorization.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(capturedRequest.method()).isEqualTo("POST");
        assertThat(capturedRequest.path()).isEqualTo(TOKEN_PATH);
        assertThat(capturedRequest.form()).containsEntry("client_id", CLIENT_ID);
        assertThat(capturedRequest.form()).containsEntry("client_secret", CLIENT_SECRET);
        assertThat(capturedRequest.form()).containsEntry("grant_type", "refresh_token");
        assertThat(capturedRequest.form()).containsEntry("refresh_token", REFRESH_TOKEN);
    }

    @Test
    void deauthorize_posts_access_token_form_request() {
        adapter.deauthorize(ACCESS_TOKEN);

        assertThat(capturedRequest.method()).isEqualTo("POST");
        assertThat(capturedRequest.path()).isEqualTo(DEAUTHORIZATION_PATH);
        assertThat(capturedRequest.form()).containsEntry("access_token", ACCESS_TOKEN);
    }

    @Test
    void exchange_failure_translates_to_safe_application_exception() {
        tokenStatusCode = 400;

        assertThatThrownBy(() -> adapter.exchangeAuthorizationCode(CODE))
                .isInstanceOf(StravaAuthorizationFailureException.class)
                .hasMessage("Strava authorization could not be completed")
                .hasMessageNotContaining(CODE)
                .hasMessageNotContaining(CLIENT_SECRET)
                .hasMessageNotContaining(ACCESS_TOKEN)
                .hasMessageNotContaining(REFRESH_TOKEN);
    }

    @Test
    void malformed_successful_token_response_translates_to_safe_application_exception() {
        tokenResponse = """
                {
                  "access_token": null,
                  "refresh_token": "refresh-token",
                  "expires_at": 1773507600,
                  "scope": "read",
                  "athlete": {
                    "id": 9001
                  }
                }
                """;

        assertThatThrownBy(() -> adapter.exchangeAuthorizationCode(CODE))
                .isInstanceOf(StravaAuthorizationFailureException.class)
                .hasMessage("Strava authorization could not be completed")
                .hasMessageNotContaining(CODE)
                .hasMessageNotContaining(CLIENT_SECRET)
                .hasMessageNotContaining(REFRESH_TOKEN);
    }

    @Test
    void deauthorization_failure_translates_to_safe_application_exception() {
        deauthorizationStatusCode = 503;

        assertThatThrownBy(() -> adapter.deauthorize(ACCESS_TOKEN))
                .isInstanceOf(StravaAuthorizationFailureException.class)
                .hasMessage("Strava authorization could not be completed")
                .hasMessageNotContaining(ACCESS_TOKEN);
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        capturedRequest = CapturedRequest.from(exchange);
        writeResponse(exchange, tokenStatusCode, tokenResponse);
    }

    private void handleDeauthorization(HttpExchange exchange) throws IOException {
        capturedRequest = CapturedRequest.from(exchange);
        writeResponse(exchange, deauthorizationStatusCode, "{}");
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private StravaApiProperties stravaApiProperties() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();

        return new StravaApiProperties(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, baseUrl + AUTHORIZATION_PATH,
                baseUrl + TOKEN_PATH, baseUrl + DEAUTHORIZATION_PATH, baseUrl + ACTIVITIES_PATH,
                Duration.ofSeconds(2), Duration.ofSeconds(5));
    }

    private record CapturedRequest(String method, String path, Map<String, String> form) {

        private static CapturedRequest from(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            return new CapturedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), form(body));
        }

        private static Map<String, String> form(String body) {
            Map<String, String> form = new HashMap<>();
            for (String pair : body.split("&")) {
                String[] nameAndValue = pair.split("=", 2);
                form.put(decode(nameAndValue[0]), decode(nameAndValue[1]));
            }

            return form;
        }

        private static String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }
}
