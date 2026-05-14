package com.sudolife.adapter.driven.api.strava;

import com.sudolife.application.service.strava.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.StravaTokenAuthorization;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class StravaOAuthAdapter implements StravaOAuthProvider {

    private static final String AUTHORIZATION_URL = "https://www.strava.com/oauth/authorize";

    private final String clientId;
    private final String redirectUri;

    public StravaOAuthAdapter(
            @Value("${strava.client-id}") String clientId,
            @Value("${strava.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    @Override
    public String buildAuthorizationUrl(StravaAuthorizationRequest request) {
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("approval_prompt", "auto")
                .queryParam("scope", request.scope())
                .queryParam("state", request.state())
                .build()
                .toUriString();
    }

    @Override
    public StravaTokenAuthorization exchangeAuthorizationCode(String code) {
        throw new UnsupportedOperationException("Strava token exchange is not implemented yet");
    }

    @Override
    public StravaTokenAuthorization refresh(String refreshToken) {
        throw new UnsupportedOperationException("Strava token refresh is not implemented yet");
    }

    @Override
    public void deauthorize(String accessToken) {
        throw new UnsupportedOperationException("Strava deauthorization is not implemented yet");
    }
}
