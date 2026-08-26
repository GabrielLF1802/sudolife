package com.sudolife.adapter.driven.mail;

import com.sudolife.application.service.user.ports.required.PasswordRecoveryMailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordRecoveryMailConfigurationIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PasswordRecoveryMailConfiguration.class)
            .withPropertyValues("password-recovery.mail.frontend-base-url=https://app.sudolife.example");

    @Test
    void non_production_context_uses_stub_password_recovery_mail_sender_without_resend_credentials() {
        contextRunner
                .withPropertyValues("password-recovery.mail.delivery=stub")
                .run(context -> assertThat(context)
                        .hasSingleBean(PasswordRecoveryMailSender.class)
                        .getBean(PasswordRecoveryMailSender.class)
                        .isInstanceOf(StubPasswordRecoveryMailSender.class));
    }

    @Test
    void production_context_uses_resend_password_recovery_mail_sender() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "password-recovery.mail.delivery=resend",
                        "password-recovery.mail.resend.api-key=re_test_key",
                        "password-recovery.mail.resend.sender=Sudolife <security@sudolife.example>"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(PasswordRecoveryMailSender.class)
                        .getBean(PasswordRecoveryMailSender.class)
                        .isInstanceOf(ResendPasswordRecoveryMailSender.class));
    }

    @Test
    void production_context_without_resend_credentials_fails_startup() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "password-recovery.mail.delivery=resend"
                )
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseMessage("Production Password Recovery mail delivery requires Resend configuration"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PasswordRecoveryMailProperties.class)
    @Import({
            PasswordRecoveryLinkBuilder.class,
            StubPasswordRecoveryMailSender.class,
            ResendPasswordRecoveryMailSender.class
    })
    static class PasswordRecoveryMailConfiguration {

    }
}
