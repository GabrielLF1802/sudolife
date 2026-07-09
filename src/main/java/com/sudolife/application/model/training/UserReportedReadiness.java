package com.sudolife.application.model.training;

public enum UserReportedReadiness {
    LOW,
    MODERATE,
    HIGH;

    public static UserReportedReadiness from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Readiness is required");
        }

        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Readiness is unsupported");
        }
    }
}
