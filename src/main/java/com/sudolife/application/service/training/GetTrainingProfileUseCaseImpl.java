package com.sudolife.application.service.training;

import com.sudolife.application.service.training.ports.provided.GetTrainingProfileUseCase;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class GetTrainingProfileUseCaseImpl implements GetTrainingProfileUseCase {

    private final TrainingProfileRepository repository;
    private final TimeProvider timeProvider;

    @Override
    public TrainingProfileResult execute(String userEmail) {
        int currentYear = timeProvider.now().atZone(ZoneOffset.UTC).getYear();

        return repository.findByUserEmail(userEmail)
                .map(profile -> TrainingProfileResult.existing(profile.getBirthYear(),
                        profile.getImportedHeartRateZones(), currentYear))
                .orElseGet(TrainingProfileResult::missing);
    }
}
