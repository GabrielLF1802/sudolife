package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaDataConsentPurpose;
import com.sudolife.application.model.strava.StravaDataConsentRecord;

public interface StravaDataConsentRepository {

    boolean existsByUserEmailAndPurposeAndConsentVersion(String userEmail, StravaDataConsentPurpose purpose, String consentVersion);

    StravaDataConsentRecord save(StravaDataConsentRecord consentRecord);
}
