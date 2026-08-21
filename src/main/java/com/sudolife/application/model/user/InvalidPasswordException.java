package com.sudolife.application.model.user;

import java.util.List;
import java.util.stream.Collectors;

public class InvalidPasswordException extends IllegalArgumentException {

    private final List<PasswordPolicyViolation> violations;

    public InvalidPasswordException(List<PasswordPolicyViolation> violations) {
        super(violations.stream()
                .map(PasswordPolicyViolation::message)
                .collect(Collectors.joining("; ")));
        this.violations = List.copyOf(violations);
    }

    public List<PasswordPolicyViolation> violations() {
        return violations;
    }
}
