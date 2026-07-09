package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.exception.InvalidCoachingProfileException;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaveCoachingProfileUseCaseImpl implements SaveCoachingProfileUseCase {

    private final CoachingProfileRepository repository;
    private final TimeProvider timeProvider;

    @Override
    public CoachingProfileResult execute(String userEmail, SaveCoachingProfileCommand command) {
        UserReportedReadiness readiness = validReadiness(command.readiness());
        Double targetDistanceKilometers = validTargetDistance(command.targetDistanceKilometers());
        LocalDate targetDate = validTargetDate(command.targetDate());
        Integer targetPaceSecondsPerKilometer = validTargetPace(command.targetPaceSecondsPerKilometer());
        Optional<CoachingProfile> existingProfile = repository.findByUserEmail(userEmail);
        Long profileId = existingProfile.map(CoachingProfile::getId).orElse(null);

        CoachingProfile savedProfile = repository.save(new CoachingProfile(
                profileId,
                userEmail,
                targetDistanceKilometers,
                targetPaceSecondsPerKilometer,
                targetDate,
                readiness,
                command.injuryConcern()
        ));

        return result(savedProfile);
    }

    private Double validTargetDistance(Double targetDistanceKilometers) {
        if (targetDistanceKilometers == null) {
            throw new InvalidCoachingProfileException("Target distance is required");
        }

        if (!Double.isFinite(targetDistanceKilometers) || targetDistanceKilometers <= 0) {
            throw new InvalidCoachingProfileException("Target distance must be greater than zero");
        }

        return targetDistanceKilometers;
    }

    private Integer validTargetPace(Integer targetPaceSecondsPerKilometer) {
        if (targetPaceSecondsPerKilometer == null) {
            return null;
        }

        if (targetPaceSecondsPerKilometer <= 0) {
            throw new InvalidCoachingProfileException("Target pace must be greater than zero");
        }

        return targetPaceSecondsPerKilometer;
    }

    private LocalDate validTargetDate(LocalDate targetDate) {
        if (targetDate == null) {
            return null;
        }

        LocalDate currentDate = timeProvider.now().atZone(ZoneOffset.UTC).toLocalDate();

        if (targetDate.isBefore(currentDate)) {
            throw new InvalidCoachingProfileException("Target date cannot be in the past");
        }

        return targetDate;
    }

    private UserReportedReadiness validReadiness(String readiness) {
        if (readiness == null || readiness.trim().isEmpty()) {
            throw new InvalidCoachingProfileException("Readiness is required");
        }

        try {
            return UserReportedReadiness.valueOf(readiness.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidCoachingProfileException("Readiness is unsupported");
        }
    }

    private CoachingProfileResult result(CoachingProfile profile) {
        return new CoachingProfileResult(
                profile.getTargetDistanceKilometers(),
                profile.getTargetPaceSecondsPerKilometer(),
                profile.getTargetDate(),
                profile.getReadiness().name(),
                profile.isInjuryConcern(),
                true
        );
    }
}
