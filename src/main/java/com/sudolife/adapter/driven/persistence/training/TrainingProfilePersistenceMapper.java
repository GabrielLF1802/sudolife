package com.sudolife.adapter.driven.persistence.training;

import com.sudolife.adapter.driven.persistence.training.entitymodel.TrainingProfileEntity;
import com.sudolife.application.model.training.TrainingProfile;
import org.springframework.stereotype.Component;

@Component
public class TrainingProfilePersistenceMapper {

    public TrainingProfileEntity toEntity(TrainingProfile domain) {
        TrainingProfileEntity entity = new TrainingProfileEntity();
        entity.setId(domain.getId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setBirthYear(domain.getBirthYear());

        return entity;
    }

    public TrainingProfile toDomain(TrainingProfileEntity entity) {
        return new TrainingProfile(entity.getId(), entity.getUserEmail(), entity.getBirthYear());
    }
}
