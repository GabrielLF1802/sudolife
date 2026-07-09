package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.exception.InvalidCoachingProfileException;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaveCoachingProfileUseCaseImplUnitTest {

    private final CoachingProfileRepository repository = mock(CoachingProfileRepository.class);
    private final TimeProvider timeProvider = () -> NOW;
    private final SaveCoachingProfileUseCaseImpl useCase = new SaveCoachingProfileUseCaseImpl(repository, timeProvider);

    @Test
    void execute_saves_current_coaching_profiles() {
        when(repository.save(any())).thenReturn(savedCoachingProfile(null, UserReportedReadiness.LOW, true));
        SaveCoachingProfileCommand command = command("LOW", true);

        CoachingProfileResult result = useCase.execute("user@sudolife.com", command);

        assertThat(result.targetDistanceKilometers()).isEqualTo(10.0);
        assertThat(result.targetPaceSecondsPerKilometer()).isEqualTo(330);
        assertThat(result.targetDate()).isEqualTo(LocalDate.parse("2026-05-12"));
        assertThat(result.readiness()).isEqualTo("LOW");
        assertThat(result.injuryConcern()).isTrue();
        assertThat(result.configured()).isTrue();
    }

    @Test
    void execute_updates_existing_coaching_profiles() {
        when(repository.findByUserEmail("user@sudolife.com"))
                .thenReturn(Optional.of(savedCoachingProfile(7L, UserReportedReadiness.MODERATE, false)));
        when(repository.save(any())).thenReturn(savedCoachingProfile(7L, UserReportedReadiness.HIGH, false));

        CoachingProfileResult result = useCase.execute("user@sudolife.com", command("HIGH", false));

        assertThat(result.readiness()).isEqualTo("HIGH");
    }

    @Test
    void execute_rejects_missing_target_distance() {
        SaveCoachingProfileCommand command = new SaveCoachingProfileCommand(null, 330, null, "LOW", false);

        assertThatThrownBy(() -> useCase.execute("user@sudolife.com", command))
                .isInstanceOf(InvalidCoachingProfileException.class)
                .hasMessage("Target distance is required");
    }

    @Test
    void execute_rejects_invalid_target_distance() {
        SaveCoachingProfileCommand command = new SaveCoachingProfileCommand(0.0, 330, null, "LOW", false);

        assertThatThrownBy(() -> useCase.execute("user@sudolife.com", command))
                .isInstanceOf(InvalidCoachingProfileException.class)
                .hasMessage("Target distance must be greater than zero");
    }

    @Test
    void execute_rejects_invalid_target_pace() {
        SaveCoachingProfileCommand command = new SaveCoachingProfileCommand(10.0, 0, null, "LOW", false);

        assertThatThrownBy(() -> useCase.execute("user@sudolife.com", command))
                .isInstanceOf(InvalidCoachingProfileException.class)
                .hasMessage("Target pace must be greater than zero");
    }

    @Test
    void execute_rejects_past_target_date() {
        SaveCoachingProfileCommand command = new SaveCoachingProfileCommand(10.0, 330, LocalDate.parse("2026-05-10"),
                "LOW", false);

        assertThatThrownBy(() -> useCase.execute("user@sudolife.com", command))
                .isInstanceOf(InvalidCoachingProfileException.class)
                .hasMessage("Target date cannot be in the past");
    }

    @Test
    void execute_rejects_unsupported_readiness() {
        SaveCoachingProfileCommand command = new SaveCoachingProfileCommand(10.0, 330, null, "TIRED", false);

        assertThatThrownBy(() -> useCase.execute("user@sudolife.com", command))
                .isInstanceOf(InvalidCoachingProfileException.class)
                .hasMessage("Readiness is unsupported");
    }

    private SaveCoachingProfileCommand command(String readiness, boolean injuryConcern) {
        return new SaveCoachingProfileCommand(10.0, 330, LocalDate.parse("2026-05-12"), readiness, injuryConcern);
    }

    private CoachingProfile savedCoachingProfile(Long id, UserReportedReadiness readiness, boolean injuryConcern) {
        return new CoachingProfile(id, "user@sudolife.com", 10.0, 330, LocalDate.parse("2026-05-12"), readiness,
                injuryConcern);
    }
}
