package com.sudolife.application.model.training;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserReportedReadinessUnitTest {

    @Test
    void from_returns_supported_readiness() {
        UserReportedReadiness readiness = UserReportedReadiness.from("MODERATE");

        assertThat(readiness).isEqualTo(UserReportedReadiness.MODERATE);
    }

    @Test
    void from_trims_supported_readiness() {
        UserReportedReadiness readiness = UserReportedReadiness.from(" HIGH ");

        assertThat(readiness).isEqualTo(UserReportedReadiness.HIGH);
    }

    @Test
    void from_rejects_missing_readiness() {
        assertThatThrownBy(() -> UserReportedReadiness.from(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Readiness is required");
    }

    @Test
    void from_rejects_unsupported_readiness() {
        assertThatThrownBy(() -> UserReportedReadiness.from("TIRED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Readiness is unsupported");
    }
}
