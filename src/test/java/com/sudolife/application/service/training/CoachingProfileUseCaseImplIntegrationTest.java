package com.sudolife.application.service.training;

import com.sudolife.application.service.training.ports.provided.GetCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@Import(FixedTimeProvider.class)
class CoachingProfileUseCaseImplIntegrationTest {

    @Autowired
    private SaveCoachingProfileUseCase saveUseCase;

    @Autowired
    private GetCoachingProfileUseCase getUseCase;

    @Test
    void execute_saves_and_retrieves_coaching_profiles_with_real_repository() {
        saveUseCase.execute("user@sudolife.com", command("LOW", true));

        CoachingProfileResult result = getUseCase.execute("user@sudolife.com");

        assertThat(result.targetDistanceKilometers()).isEqualTo(10.0);
        assertThat(result.targetPaceSecondsPerKilometer()).isEqualTo(330);
        assertThat(result.targetDate()).isEqualTo(LocalDate.parse("2026-05-12"));
        assertThat(result.readiness()).isEqualTo("LOW");
        assertThat(result.injuryConcern()).isTrue();
    }

    @Test
    void execute_rejects_invalid_coaching_profiles_with_real_repository() {
        SaveCoachingProfileCommand command = new SaveCoachingProfileCommand(10.0, 330, null, "UNSUPPORTED", false);

        assertThatThrownBy(() -> saveUseCase.execute("user@sudolife.com", command))
                .hasMessage("Readiness is unsupported");
    }

    private SaveCoachingProfileCommand command(String readiness, boolean injuryConcern) {
        return new SaveCoachingProfileCommand(10.0, 330, LocalDate.parse("2026-05-12"), readiness, injuryConcern);
    }
}
