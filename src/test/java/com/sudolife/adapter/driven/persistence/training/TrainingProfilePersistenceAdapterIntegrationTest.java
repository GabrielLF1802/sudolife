package com.sudolife.adapter.driven.persistence.training;

import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class TrainingProfilePersistenceAdapterIntegrationTest {

    @Autowired
    private TrainingProfileRepository repository;

    @Test
    void save_persists_training_profile() {
        TrainingProfile savedProfile = repository.save(new TrainingProfile(null, "user@sudolife.com", 1990));

        Optional<TrainingProfile> result = repository.findByUserEmail("user@sudolife.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedProfile.getId());
        assertThat(result.get().getBirthYear()).isEqualTo(1990);
    }
}
