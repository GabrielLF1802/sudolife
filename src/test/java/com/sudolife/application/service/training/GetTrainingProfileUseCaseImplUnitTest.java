package com.sudolife.application.service.training;

import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetTrainingProfileUseCaseImplUnitTest {

    private final TrainingProfileRepository repository = mock(TrainingProfileRepository.class);
    private final GetTrainingProfileUseCaseImpl useCase = new GetTrainingProfileUseCaseImpl(repository,
            () -> Instant.parse("2026-05-10T12:00:00Z"));

    @Test
    void execute_returns_missing_profile_when_user_has_no_birth_year() {
        when(repository.findByUserEmail("user@sudolife.com")).thenReturn(Optional.empty());

        TrainingProfileResult result = useCase.execute("user@sudolife.com");

        assertThat(result.birthYear()).isNull();
        assertThat(result.adaptiveCoachingEligible()).isFalse();
        assertThat(result.heartRateZoneSource()).isEqualTo(TrainingHeartRateZoneSource.UNAVAILABLE);
        assertThat(result.heartRateZones()).isEmpty();
    }

    @Test
    void execute_returns_eligible_profile_when_birth_year_exists() {
        when(repository.findByUserEmail("user@sudolife.com"))
                .thenReturn(Optional.of(new TrainingProfile(1L, "user@sudolife.com", 1990)));

        TrainingProfileResult result = useCase.execute("user@sudolife.com");

        assertThat(result.birthYear()).isEqualTo(1990);
        assertThat(result.adaptiveCoachingEligible()).isTrue();
        assertThat(result.heartRateZoneSource()).isEqualTo(TrainingHeartRateZoneSource.AGE_BASED);
        assertThat(result.heartRateZones()).containsExactly(
                new TrainingHeartRateZone(92, 110),
                new TrainingHeartRateZone(110, 128),
                new TrainingHeartRateZone(128, 147),
                new TrainingHeartRateZone(147, 165),
                new TrainingHeartRateZone(165, 184)
        );
    }

    @Test
    void execute_prefers_imported_strava_heart_rate_zones() {
        List<TrainingHeartRateZone> importedZones = importedZones();
        when(repository.findByUserEmail("user@sudolife.com"))
                .thenReturn(Optional.of(new TrainingProfile(1L, "user@sudolife.com", 1990, importedZones)));

        TrainingProfileResult result = useCase.execute("user@sudolife.com");

        assertThat(result.heartRateZoneSource()).isEqualTo(TrainingHeartRateZoneSource.STRAVA);
        assertThat(result.heartRateZones()).containsExactlyElementsOf(importedZones);
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
