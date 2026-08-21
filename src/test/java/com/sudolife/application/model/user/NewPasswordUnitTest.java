package com.sudolife.application.model.user;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewPasswordUnitTest {

    private static final PasswordContext CONTEXT = new PasswordContext(
            "runner@sudolife.com",
            "runner",
            "Alex Taylor",
            "Alex",
            LocalDate.of(1990, 4, 12)
    );

    @Test
    void constructor_accepts_valid_password() {
        NewPassword password = new NewPassword("Str0ng!Password", CONTEXT);

        assertThat(password.value()).isEqualTo("Str0ng!Password");
    }

    @Test
    void constructor_rejects_blank_password() {
        assertThatThrownBy(() -> new NewPassword("   ", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.BLANK);
    }

    @Test
    void constructor_rejects_password_shorter_than_twelve_characters() {
        assertThatThrownBy(() -> new NewPassword("Str0ng!Pass", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.TOO_SHORT);
    }

    @Test
    void constructor_rejects_password_longer_than_one_hundred_twenty_eight_characters() {
        String password = "A1!" + "a".repeat(126);

        assertThatThrownBy(() -> new NewPassword(password, CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.TOO_LONG);
    }

    @Test
    void constructor_rejects_password_without_uppercase_letter() {
        assertThatThrownBy(() -> new NewPassword("str0ng!password", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.MISSING_UPPERCASE);
    }

    @Test
    void constructor_rejects_password_without_lowercase_letter() {
        assertThatThrownBy(() -> new NewPassword("STR0NG!PASSWORD", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.MISSING_LOWERCASE);
    }

    @Test
    void constructor_rejects_password_without_number() {
        assertThatThrownBy(() -> new NewPassword("Strong!Password", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.MISSING_NUMBER);
    }

    @Test
    void constructor_rejects_password_without_special_character() {
        assertThatThrownBy(() -> new NewPassword("Str0ngPassword", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.MISSING_SPECIAL_CHARACTER);
    }

    @Test
    void constructor_rejects_password_containing_email() {
        assertThatThrownBy(() -> new NewPassword("runner@sudolife.comA1!", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.CONTAINS_CONTEXTUAL_DATA);
    }

    @Test
    void constructor_rejects_password_containing_username() {
        assertThatThrownBy(() -> new NewPassword("runnerStrong1!", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.CONTAINS_CONTEXTUAL_DATA);
    }

    @Test
    void constructor_rejects_password_containing_full_name() {
        assertThatThrownBy(() -> new NewPassword("Alex Taylor1!", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.CONTAINS_CONTEXTUAL_DATA);
    }

    @Test
    void constructor_rejects_password_containing_name() {
        assertThatThrownBy(() -> new NewPassword("AlexStrong1!", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.CONTAINS_CONTEXTUAL_DATA);
    }

    @Test
    void constructor_rejects_password_containing_birth_date() {
        assertThatThrownBy(() -> new NewPassword("Birth1990-04-12!", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting("violations")
                .asList()
                .contains(PasswordPolicyViolation.CONTAINS_CONTEXTUAL_DATA);
    }

    @Test
    void constructor_returns_detailed_validation_failures() {
        assertThatThrownBy(() -> new NewPassword("password", CONTEXT))
                .isInstanceOf(InvalidPasswordException.class)
                .satisfies(exception -> assertThat(((InvalidPasswordException) exception).violations())
                        .contains(
                                PasswordPolicyViolation.TOO_SHORT,
                                PasswordPolicyViolation.MISSING_UPPERCASE,
                                PasswordPolicyViolation.MISSING_NUMBER,
                                PasswordPolicyViolation.MISSING_SPECIAL_CHARACTER
                        ));
    }
}
