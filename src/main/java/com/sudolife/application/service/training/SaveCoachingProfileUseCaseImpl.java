package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.RunningGoal;
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
        RunningGoal runningGoal = runningGoal(command);
        UserReportedReadiness readiness = readiness(command);
        Optional<CoachingProfile> existingProfile = repository.findByUserEmail(userEmail);
        Long profileId = existingProfile.map(CoachingProfile::getId).orElse(null);

        CoachingProfile savedProfile = repository.save(new CoachingProfile(
                profileId,
                userEmail,
                runningGoal,
                readiness,
                command.injuryConcern()
        ));

        return result(savedProfile);
    }

    private RunningGoal runningGoal(SaveCoachingProfileCommand command) {
        LocalDate currentDate = null;

        if (command.targetDate() != null) {
            currentDate = timeProvider.now().atZone(ZoneOffset.UTC).toLocalDate();
        }

        try {
            return RunningGoal.createFromUserInput(
                    command.targetDistanceKilometers(),
                    command.targetPaceSecondsPerKilometer(),
                    command.targetDate(),
                    currentDate
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidCoachingProfileException(exception.getMessage());
        }
    }

    private UserReportedReadiness readiness(SaveCoachingProfileCommand command) {
        try {
            return UserReportedReadiness.from(command.readiness());
        } catch (IllegalArgumentException exception) {
            throw new InvalidCoachingProfileException(exception.getMessage());
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
