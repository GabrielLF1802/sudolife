package com.sudolife.adapter.driven.persistence.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class CoachingProfilePersistenceAdapterIntegrationTest {

    @Autowired
    private CoachingProfileRepository repository;

    @Test
    void save_persists_coaching_profiles() {
        CoachingProfile savedProfile = repository.save(profile(null));

        Optional<CoachingProfile> result = repository.findByUserEmail("user@sudolife.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedProfile.getId());
        assertThat(result.get().getTargetDistanceKilometers()).isEqualTo(21.1);
        assertThat(result.get().getTargetPaceSecondsPerKilometer()).isEqualTo(360);
        assertThat(result.get().getTargetDate()).isEqualTo(LocalDate.parse("2026-06-01"));
        assertThat(result.get().getReadiness()).isEqualTo(UserReportedReadiness.MODERATE);
        assertThat(result.get().isInjuryConcern()).isTrue();
    }

    @Test
    void save_updates_existing_coaching_profiles_for_user() {
        CoachingProfile savedProfile = repository.save(profile(null));

        repository.save(new CoachingProfile(
                savedProfile.getId(),
                "user@sudolife.com",
                new RunningGoal(42.2, null, null),
                UserReportedReadiness.HIGH,
                false
        ));
        Optional<CoachingProfile> result = repository.findByUserEmail("user@sudolife.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedProfile.getId());
        assertThat(result.get().getTargetDistanceKilometers()).isEqualTo(42.2);
        assertThat(result.get().getTargetPaceSecondsPerKilometer()).isNull();
        assertThat(result.get().getTargetDate()).isNull();
        assertThat(result.get().getReadiness()).isEqualTo(UserReportedReadiness.HIGH);
        assertThat(result.get().isInjuryConcern()).isFalse();
    }

    private CoachingProfile profile(Long id) {
        return new CoachingProfile(
                id,
                "user@sudolife.com",
                new RunningGoal(21.1, 360, LocalDate.parse("2026-06-01")),
                UserReportedReadiness.MODERATE,
                true
        );
    }
}
