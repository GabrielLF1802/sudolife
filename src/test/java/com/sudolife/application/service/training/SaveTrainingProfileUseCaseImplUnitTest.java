package com.sudolife.application.service.training;

import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.exception.InvalidTrainingProfileException;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaveTrainingProfileUseCaseImplUnitTest {

    private final TrainingProfileRepository repository = mock(TrainingProfileRepository.class);
    private final TimeProvider timeProvider = () -> Instant.parse("2026-05-10T12:00:00Z");
    private final SaveTrainingProfileUseCaseImpl useCase = new SaveTrainingProfileUseCaseImpl(repository, timeProvider);

    @Test
    void execute_saves_birth_year_for_new_profile() {
        when(repository.findByUserEmail("user@sudolife.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(new TrainingProfile(1L, "user@sudolife.com", 1990));

        TrainingProfileResult result = useCase.execute("user@sudolife.com", new SaveTrainingProfileCommand(1990));

        assertThat(result.birthYear()).isEqualTo(1990);
        assertThat(result.adaptiveCoachingEligible()).isTrue();
    }

    @Test
    void execute_updates_existing_profile() {
        when(repository.findByUserEmail("user@sudolife.com"))
                .thenReturn(Optional.of(new TrainingProfile(7L, "user@sudolife.com", 1988)));
        when(repository.save(any())).thenReturn(new TrainingProfile(7L, "user@sudolife.com", 1991));

        TrainingProfileResult result = useCase.execute("user@sudolife.com", new SaveTrainingProfileCommand(1991));

        assertThat(result.birthYear()).isEqualTo(1991);
    }

    @Test
    void execute_rejects_missing_birth_year() {
        SaveTrainingProfileCommand command = new SaveTrainingProfileCommand(null);

        assertThatThrownBy(() -> useCase.execute("user@sudolife.com", command))
                .isInstanceOf(InvalidTrainingProfileException.class)
                .hasMessage("Birth year is required");
    }

    @Test
    void execute_rejects_future_birth_year() {
        SaveTrainingProfileCommand command = new SaveTrainingProfileCommand(2027);

        assertThatThrownBy(() -> useCase.execute("user@sudolife.com", command))
                .isInstanceOf(InvalidTrainingProfileException.class)
                .hasMessage("Birth year cannot be in the future");
    }

    @Test
    void execute_rejects_implausible_birth_year() {
        SaveTrainingProfileCommand command = new SaveTrainingProfileCommand(1905);

        assertThatThrownBy(() -> useCase.execute("user@sudolife.com", command))
                .isInstanceOf(InvalidTrainingProfileException.class)
                .hasMessage("Birth year is implausible");
    }
}
