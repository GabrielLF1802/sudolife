package com.sudolife.application.service.training;

import com.sudolife.application.service.training.ports.provided.GetTrainingProfileUseCase;
import com.sudolife.application.service.training.ports.provided.SaveTrainingProfileUseCase;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@Import(FixedTimeProvider.class)
class TrainingProfileUseCaseImplIntegrationTest {

    @Autowired
    private SaveTrainingProfileUseCase saveUseCase;

    @Autowired
    private GetTrainingProfileUseCase getUseCase;

    @Test
    void execute_saves_and_retrieves_training_profile_with_real_repository() {
        saveUseCase.execute("user@sudolife.com", new SaveTrainingProfileCommand(1990));

        TrainingProfileResult result = getUseCase.execute("user@sudolife.com");

        assertThat(result.birthYear()).isEqualTo(1990);
        assertThat(result.adaptiveCoachingEligible()).isTrue();
        assertThat(result.heartRateZoneSource()).isEqualTo(TrainingHeartRateZoneSource.AGE_BASED);
    }

    @Test
    void execute_rejects_implausible_training_profile_with_real_repository() {
        SaveTrainingProfileCommand command = new SaveTrainingProfileCommand(1905);

        assertThatThrownBy(() -> saveUseCase.execute("user@sudolife.com", command))
                .hasMessage("Birth year is implausible");
    }
}
