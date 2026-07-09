package com.sudolife.adapter.driven.persistence.training;

import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

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

    @Test
    void save_persists_imported_heart_rate_zones_without_birth_year() {
        List<TrainingHeartRateZone> zones = importedZones();
        TrainingProfile savedProfile = repository.save(new TrainingProfile(null, "user@sudolife.com", null, zones));

        Optional<TrainingProfile> result = repository.findByUserEmail("user@sudolife.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedProfile.getId());
        assertThat(result.get().getBirthYear()).isNull();
        assertThat(result.get().getImportedHeartRateZones()).containsExactlyElementsOf(zones);
    }

    private List<TrainingHeartRateZone> importedZones() {
        return List.of(
                new TrainingHeartRateZone(100, 120),
                new TrainingHeartRateZone(121, 140),
                new TrainingHeartRateZone(141, 160),
                new TrainingHeartRateZone(161, 180),
                new TrainingHeartRateZone(181, 200)
        );
    }
}
