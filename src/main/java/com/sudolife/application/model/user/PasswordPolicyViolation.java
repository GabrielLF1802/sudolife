package com.sudolife.application.model.user;

public enum PasswordPolicyViolation {
    BLANK("Password must not be blank"),
    TOO_SHORT("Password must be at least 12 characters long"),
    TOO_LONG("Password must be at most 128 characters long"),
    MISSING_UPPERCASE("Password must include at least one uppercase letter"),
    MISSING_LOWERCASE("Password must include at least one lowercase letter"),
    MISSING_NUMBER("Password must include at least one number"),
    MISSING_SPECIAL_CHARACTER("Password must include at least one special character"),
    CONTAINS_CONTEXTUAL_DATA("Password must not contain personal information");

    private final String message;

    PasswordPolicyViolation(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
