package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetCoachingProfileUseCaseImplUnitTest {

    private final CoachingProfileRepository repository = mock(CoachingProfileRepository.class);
    private final GetCoachingProfileUseCaseImpl useCase = new GetCoachingProfileUseCaseImpl(repository);

    @Test
    void execute_returns_missing_result_when_inputs_do_not_exist() {
        CoachingProfileResult result = useCase.execute("user@sudolife.com");

        assertThat(result.configured()).isFalse();
        assertThat(result.targetDistanceKilometers()).isNull();
        assertThat(result.readiness()).isNull();
    }

    @Test
    void execute_returns_existing_coaching_profiles() {
        when(repository.findByUserEmail("user@sudolife.com"))
                .thenReturn(Optional.of(profile()));

        CoachingProfileResult result = useCase.execute("user@sudolife.com");

        assertThat(result.targetDistanceKilometers()).isEqualTo(21.1);
        assertThat(result.targetPaceSecondsPerKilometer()).isEqualTo(360);
        assertThat(result.targetDate()).isEqualTo(LocalDate.parse("2026-06-01"));
        assertThat(result.readiness()).isEqualTo("MODERATE");
        assertThat(result.injuryConcern()).isTrue();
        assertThat(result.configured()).isTrue();
    }

    private CoachingProfile profile() {
        return new CoachingProfile(
                3L,
                "user@sudolife.com",
                new RunningGoal(21.1, 360, LocalDate.parse("2026-06-01")),
                UserReportedReadiness.MODERATE,
                true
        );
    }
}
