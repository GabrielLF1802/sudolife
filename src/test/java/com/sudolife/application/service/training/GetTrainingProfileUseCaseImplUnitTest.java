package com.sudolife.application.service.training;

import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetTrainingProfileUseCaseImplUnitTest {

    private final TrainingProfileRepository repository = mock(TrainingProfileRepository.class);
    private final GetTrainingProfileUseCaseImpl useCase = new GetTrainingProfileUseCaseImpl(repository);

    @Test
    void execute_returns_missing_profile_when_user_has_no_birth_year() {
        when(repository.findByUserEmail("user@sudolife.com")).thenReturn(Optional.empty());

        TrainingProfileResult result = useCase.execute("user@sudolife.com");

        assertThat(result.birthYear()).isNull();
        assertThat(result.adaptiveCoachingEligible()).isFalse();
    }

    @Test
    void execute_returns_eligible_profile_when_birth_year_exists() {
        when(repository.findByUserEmail("user@sudolife.com"))
                .thenReturn(Optional.of(new TrainingProfile(1L, "user@sudolife.com", 1990)));

        TrainingProfileResult result = useCase.execute("user@sudolife.com");

        assertThat(result.birthYear()).isEqualTo(1990);
        assertThat(result.adaptiveCoachingEligible()).isTrue();
    }
}
