package com.sudolife.adapter.driven.api.strava;

import com.sudolife.adapter.driven.api.strava.dto.StravaTokenResponse;
import com.sudolife.application.service.strava.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.StravaTokenAuthorization;
import com.sudolife.application.service.strava.exception.StravaAuthorizationFailureException;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
public class StravaOAuthAdapter implements StravaOAuthProvider {

    private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
    private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";

    private final StravaApiProperties properties;
    private final RestClient restClient;

    @Autowired
    public StravaOAuthAdapter(StravaApiProperties properties) {
        this(properties, RestClient.builder()
                .requestFactory(requestFactory(properties))
                .build());
    }

    StravaOAuthAdapter(StravaApiProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String buildAuthorizationUrl(StravaAuthorizationRequest request) {
        return UriComponentsBuilder.fromUriString(properties.authorizationUrl())
                .queryParam("client_id", encoded(properties.clientId()))
                .queryParam("redirect_uri", encoded(properties.redirectUri()))
                .queryParam("response_type", encoded("code"))
                .queryParam("approval_prompt", encoded("auto"))
                .queryParam("scope", encoded(request.scope()))
                .queryParam("state", encoded(request.state()))
                .build(true)
                .toUriString();
    }

    @Override
    public StravaTokenAuthorization exchangeAuthorizationCode(String code) {
        MultiValueMap<String, String> form = commonTokenForm(AUTHORIZATION_CODE_GRANT_TYPE);
        form.add("code", code);

        return requestToken(form, "token_exchange");
    }

    @Override
    public StravaTokenAuthorization refresh(String refreshToken) {
        MultiValueMap<String, String> form = commonTokenForm(REFRESH_TOKEN_GRANT_TYPE);
        form.add("refresh_token", refreshToken);

        return requestToken(form, "token_refresh");
    }

    @Override
    public void deauthorize(String accessToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("access_token", accessToken);

        try {
            restClient.post()
                    .uri(properties.deauthorizationUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.warn("Strava deauthorization failed statusCode={}", response.getStatusCode().value());
                        throw new StravaAuthorizationFailureException();
                    })
                    .toBodilessEntity();
        } catch (StravaAuthorizationFailureException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Strava deauthorization request failed category=client_error");
            throw new StravaAuthorizationFailureException(exception);
        }
    }

    private StravaTokenAuthorization requestToken(MultiValueMap<String, String> form, String operation) {
        try {
            StravaTokenResponse response = restClient.post()
                    .uri(properties.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                        log.warn("Strava OAuth request failed operation={} statusCode={}", operation,
                                httpResponse.getStatusCode().value());
                        throw new StravaAuthorizationFailureException();
                    })
                    .body(StravaTokenResponse.class);

            return toAuthorization(response, operation);
        } catch (StravaAuthorizationFailureException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Strava OAuth request failed operation={} category=client_error", operation);
            throw new StravaAuthorizationFailureException(exception);
        } catch (RuntimeException exception) {
            log.warn("Strava OAuth response mapping failed operation={}", operation);
            throw new StravaAuthorizationFailureException(exception);
        }
    }

    private StravaTokenAuthorization toAuthorization(StravaTokenResponse response, String operation) {
        if (invalidTokenResponse(response, operation)) {
            throw new StravaAuthorizationFailureException();
        }

        Long athleteId = response.athlete() == null ? null : response.athlete().id();

        return new StravaTokenAuthorization(athleteId, response.accessToken(), response.refreshToken(),
                Instant.ofEpochSecond(response.expiresAt()), response.scope());
    }

    private boolean invalidTokenResponse(StravaTokenResponse response, String operation) {
        if (response == null || !hasText(response.accessToken()) || !hasText(response.refreshToken())
                || response.expiresAt() == null) {
            return true;
        }

        return "token_exchange".equals(operation) && (response.athlete() == null || response.athlete().id() == null);
    }

    private MultiValueMap<String, String> commonTokenForm(String grantType) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("grant_type", grantType);

        return form;
    }

    private static SimpleClientHttpRequestFactory requestFactory(StravaApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return requestFactory;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
