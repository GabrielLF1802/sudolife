package com.sudolife.adapter.driven.persistence.training.coaching;

import com.sudolife.adapter.driven.persistence.training.coaching.entitymodel.CoachingProfileEntity;
import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.UserReportedReadiness;
import org.springframework.stereotype.Component;

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

        return entity;
    }

    public CoachingProfile toDomain(CoachingProfileEntity entity) {
        return new CoachingProfile(
                entity.getId(),
                entity.getUserEmail(),
                entity.getTargetDistanceKilometers(),
                entity.getTargetPaceSecondsPerKilometer(),
                entity.getTargetDate(),
                UserReportedReadiness.valueOf(entity.getReadiness()),
                entity.isInjuryConcern()
        );
    }
}
