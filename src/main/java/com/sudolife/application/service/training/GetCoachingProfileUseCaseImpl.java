package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.service.training.ports.provided.GetCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCoachingProfileUseCaseImpl implements GetCoachingProfileUseCase {

    private final CoachingProfileRepository repository;

    @Override
    public CoachingProfileResult execute(String userEmail) {
        return repository.findByUserEmail(userEmail)
                .map(this::result)
                .orElseGet(CoachingProfileResult::missing);
    }

    private CoachingProfileResult result(CoachingProfile profile) {
        return new CoachingProfileResult(
                profile.getTargetDistanceKilometers(),
                profile.getTargetPaceSecondsPerKilometer(),
                profile.getTargetDate(),
                profile.getReadiness().name(),
                profile.isInjuryConcern(),
                profile.getRunningAvailability().getPreferredRunningDays().stream()
                        .map(java.time.DayOfWeek::name)
                        .toList(),
                true
        );
    }
}
