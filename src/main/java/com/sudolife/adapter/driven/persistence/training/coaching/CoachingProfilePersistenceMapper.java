package com.sudolife.adapter.driven.persistence.training.coaching;

import com.sudolife.adapter.driven.persistence.training.coaching.entitymodel.CoachingProfileEntity;
import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.model.training.RunningAvailability;
import com.sudolife.application.model.training.UserReportedReadiness;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

@Component
public class CoachingProfilePersistenceMapper {

    public CoachingProfileEntity toEntity(CoachingProfile domain) {
        CoachingProfileEntity entity = new CoachingProfileEntity();
        entity.setId(domain.getId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setTargetDistanceKilometers(domain.getTargetDistanceKilometers());
        entity.setTargetPaceSecondsPerKilometer(domain.getTargetPaceSecondsPerKilometer());
        entity.setTargetDate(domain.getTargetDate());
        entity.setReadiness(domain.getReadiness().name());
        entity.setInjuryConcern(domain.isInjuryConcern());
        entity.setPreferredRunningDays(String.join(",", domain.getRunningAvailability().getPreferredRunningDays()
                .stream().map(DayOfWeek::name).toList()));

        return entity;
    }

    public CoachingProfile toDomain(CoachingProfileEntity entity) {
        return new CoachingProfile(
                entity.getId(),
                entity.getUserEmail(),
                new RunningGoal(
                        entity.getTargetDistanceKilometers(),
                        entity.getTargetPaceSecondsPerKilometer(),
                        entity.getTargetDate()
                ),
                UserReportedReadiness.valueOf(entity.getReadiness()),
                entity.isInjuryConcern(),
                new RunningAvailability(preferredRunningDays(entity.getPreferredRunningDays()))
        );
    }

    private List<DayOfWeek> preferredRunningDays(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(DayOfWeek::valueOf)
                .toList();
    }
}
