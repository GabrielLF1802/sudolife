package com.sudolife.application.model.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record PasswordContext(String email, String username, String fullName, String name, LocalDate birthDate) {

    public static PasswordContext registration(String email, String name) {
        return new PasswordContext(email, emailUsername(email), name, name, null);
    }

    public List<String> values() {
        return Stream.of(
                        Optional.ofNullable(email),
                        Optional.ofNullable(username),
                        Optional.ofNullable(fullName),
                        Optional.ofNullable(name),
                        birthDateValue()
                )
                .flatMap(Optional::stream)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private Optional<String> birthDateValue() {
        return Optional.ofNullable(birthDate).map(LocalDate::toString);
    }

    private static String emailUsername(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        return email.substring(0, email.indexOf('@'));
    }
}
