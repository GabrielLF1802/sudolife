package com.sudolife.application.service.strava.consent;

import com.sudolife.application.model.strava.StravaDataConsentPurpose;
import com.sudolife.application.model.strava.StravaDataConsentVersions;
import com.sudolife.application.service.strava.ports.provided.GetStravaDataConsentStatusUseCase;
import com.sudolife.application.service.strava.ports.required.StravaDataConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStravaDataConsentStatusUseCaseImpl implements GetStravaDataConsentStatusUseCase {

    private final StravaDataConsentRepository consentRepository;

    @Override
    public StravaDataConsentStatusResult execute(GetStravaDataConsentStatusCommand command) {
        boolean valid = consentRepository.existsByUserEmailAndPurposeAndConsentVersion(command.userEmail(),
                StravaDataConsentPurpose.STRAVA_DATA_IMPORT_AND_COACHING, StravaDataConsentVersions.CURRENT);

        return new StravaDataConsentStatusResult(valid, StravaDataConsentVersions.CURRENT,
                StravaDataConsentPurpose.STRAVA_DATA_IMPORT_AND_COACHING.name());
    }
}
