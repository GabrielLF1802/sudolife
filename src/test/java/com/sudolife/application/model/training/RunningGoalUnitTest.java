package com.sudolife.application.model.training;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunningGoalUnitTest {

    @Test
    void constructor_keeps_valid_goal_values() {
        LocalDate targetDate = LocalDate.parse("2026-05-12");

        RunningGoal goal = new RunningGoal(10.0, 330, targetDate);

        assertThat(goal.getTargetDistanceKilometers()).isEqualTo(10.0);
        assertThat(goal.getTargetPaceSecondsPerKilometer()).isEqualTo(330);
        assertThat(goal.getTargetDate()).isEqualTo(targetDate);
    }

    @Test
    void constructor_rejects_missing_target_distance() {
        assertThatThrownBy(() -> new RunningGoal(null, 330, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target distance is required");
    }

    @Test
    void constructor_rejects_invalid_target_distance() {
        assertThatThrownBy(() -> new RunningGoal(0.0, 330, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target distance must be greater than zero");
    }

    @Test
    void constructor_rejects_invalid_target_pace() {
        assertThatThrownBy(() -> new RunningGoal(10.0, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target pace must be greater than zero");
    }

    @Test
    void create_from_user_input_accepts_target_date_on_current_date() {
        LocalDate currentDate = LocalDate.parse("2026-05-12");

        RunningGoal goal = RunningGoal.createFromUserInput(10.0, null, currentDate, currentDate);

        assertThat(goal.getTargetDate()).isEqualTo(currentDate);
    }

    @Test
    void create_from_user_input_rejects_past_target_date() {
        LocalDate currentDate = LocalDate.parse("2026-05-12");

        assertThatThrownBy(() -> RunningGoal.createFromUserInput(10.0, null, LocalDate.parse("2026-05-11"),
                currentDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target date cannot be in the past");
    }
}
