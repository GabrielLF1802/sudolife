package com.sudolife.application.service.strava.consent;

import com.sudolife.application.model.strava.StravaDataConsentRecord;
import com.sudolife.application.service.strava.ports.provided.RecordStravaDataConsentUseCase;
import com.sudolife.application.service.strava.ports.required.StravaDataConsentRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordStravaDataConsentUseCaseImpl implements RecordStravaDataConsentUseCase {

    private final StravaDataConsentRepository consentRepository;
    private final TimeProvider timeProvider;

    @Override
    public void execute(RecordStravaDataConsentCommand command) {
        consentRepository.save(StravaDataConsentRecord.accepted(command.userEmail(), command.language(),
                timeProvider.now()));
    }
}
