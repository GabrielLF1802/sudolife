package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.service.strava.authorization.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.authorization.StravaTokenAuthorization;

public interface StravaOAuthProvider {

    String buildAuthorizationUrl(StravaAuthorizationRequest request);

    StravaTokenAuthorization exchangeAuthorizationCode(String code);

    StravaTokenAuthorization refresh(String refreshToken);

    void deauthorize(String accessToken);
}
