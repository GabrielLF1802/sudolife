package com.sudolife.application.service.training;

import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.exception.InvalidTrainingProfileException;
import com.sudolife.application.service.training.ports.provided.SaveTrainingProfileUseCase;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaveTrainingProfileUseCaseImpl implements SaveTrainingProfileUseCase {

    private static final int MAX_PLAUSIBLE_AGE = 110;

    private final TrainingProfileRepository repository;
    private final TimeProvider timeProvider;

    @Override
    public TrainingProfileResult execute(String userEmail, SaveTrainingProfileCommand command) {
        int birthYear = validBirthYear(command.birthYear());
        Optional<TrainingProfile> existingProfile = repository.findByUserEmail(userEmail);
        Long profileId = existingProfile.map(TrainingProfile::getId).orElse(null);
        List<TrainingHeartRateZone> importedHeartRateZones = existingProfile
                .map(TrainingProfile::getImportedHeartRateZones)
                .orElse(List.of());

        TrainingProfile savedProfile = repository.save(new TrainingProfile(profileId, userEmail, birthYear,
                importedHeartRateZones));
        int currentYear = timeProvider.now().atZone(ZoneOffset.UTC).getYear();

        return TrainingProfileResult.existing(savedProfile.getBirthYear(), savedProfile.getImportedHeartRateZones(),
                currentYear);
    }

    private int validBirthYear(Integer birthYear) {
        if (birthYear == null) {
            throw new InvalidTrainingProfileException("Birth year is required");
        }

        int currentYear = timeProvider.now().atZone(ZoneOffset.UTC).getYear();

        if (birthYear > currentYear) {
            throw new InvalidTrainingProfileException("Birth year cannot be in the future");
        }

        if (birthYear < currentYear - MAX_PLAUSIBLE_AGE) {
            throw new InvalidTrainingProfileException("Birth year is implausible");
        }

        return birthYear;
    }
}
