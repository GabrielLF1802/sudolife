package com.sudolife.application.service.training;

import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.exception.InvalidTrainingProfileException;
import com.sudolife.application.service.training.ports.provided.SaveTrainingProfileUseCase;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SaveTrainingProfileUseCaseImpl implements SaveTrainingProfileUseCase {

    private static final int MAX_PLAUSIBLE_AGE = 120;

    private final TrainingProfileRepository repository;
    private final TimeProvider timeProvider;

    @Override
    public TrainingProfileResult execute(String userEmail, SaveTrainingProfileCommand command) {
        int birthYear = validBirthYear(command.birthYear());
        Long profileId = repository.findByUserEmail(userEmail)
                .map(TrainingProfile::getId)
                .orElse(null);

        TrainingProfile savedProfile = repository.save(new TrainingProfile(profileId, userEmail, birthYear));

        return TrainingProfileResult.existing(savedProfile.getBirthYear());
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
