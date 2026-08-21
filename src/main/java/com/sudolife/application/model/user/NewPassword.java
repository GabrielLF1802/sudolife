package com.sudolife.application.model.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NewPassword {

    private static final int MINIMUM_LENGTH = 12;
    private static final int MAXIMUM_LENGTH = 128;

    private final String value;

    public NewPassword(String value, PasswordContext context) {
        this.value = normalize(value);
        List<PasswordPolicyViolation> violations = validate(this.value, context);

        if (!violations.isEmpty()) {
            throw new InvalidPasswordException(violations);
        }
    }

    public String value() {
        return value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    private static List<PasswordPolicyViolation> validate(String value, PasswordContext context) {
        List<PasswordPolicyViolation> violations = validateCore(value);

        if (containsContextualData(value, context)) {
            violations.add(PasswordPolicyViolation.CONTAINS_CONTEXTUAL_DATA);
        }

        return violations;
    }

    private static List<PasswordPolicyViolation> validateCore(String value) {
        List<PasswordPolicyViolation> violations = new ArrayList<>();

        if (value.isBlank()) {
            violations.add(PasswordPolicyViolation.BLANK);
        }

        if (value.length() < MINIMUM_LENGTH) {
            violations.add(PasswordPolicyViolation.TOO_SHORT);
        }

        if (value.length() > MAXIMUM_LENGTH) {
            violations.add(PasswordPolicyViolation.TOO_LONG);
        }

        if (value.chars().noneMatch(Character::isUpperCase)) {
            violations.add(PasswordPolicyViolation.MISSING_UPPERCASE);
        }

        if (value.chars().noneMatch(Character::isLowerCase)) {
            violations.add(PasswordPolicyViolation.MISSING_LOWERCASE);
        }

        if (value.chars().noneMatch(Character::isDigit)) {
            violations.add(PasswordPolicyViolation.MISSING_NUMBER);
        }

        if (value.chars().allMatch(Character::isLetterOrDigit)) {
            violations.add(PasswordPolicyViolation.MISSING_SPECIAL_CHARACTER);
        }

        return violations;
    }

    private static boolean containsContextualData(String password, PasswordContext context) {
        if (context == null || password.isBlank()) {
            return false;
        }

        String normalizedPassword = password.toLowerCase(Locale.ROOT);

        return context.values().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedPassword::contains);
    }
}
