package com.sudolife.adapter.driven.mail;

import com.sudolife.application.service.user.PasswordRecoveryEmail;
import com.sudolife.application.service.user.exception.PasswordRecoveryMailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendPasswordRecoveryMailSenderUnitTest {

    private static final String API_URL = "https://resend.test";
    private static final String API_KEY = "re_test_key";
    private static final String SENDER = "Sudolife <security@sudolife.example>";
    private static final String FRONTEND_BASE_URL = "https://app.sudolife.example";
    private static final String EMAIL = "runner@sudolife.com";
    private static final String TOKEN = "raw-token-with secret";

    @Test
    void send_posts_password_recovery_email_to_resend() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(API_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        ResendPasswordRecoveryMailSender sender = mailSender(restClientBuilder.build());
        server.expect(requestTo(API_URL + "/emails"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"from\":\"" + SENDER + "\"")))
                .andExpect(content().string(containsString("\"to\":[\"" + EMAIL + "\"]")))
                .andExpect(content().string(containsString("\"subject\":\"Recupere sua senha no Sudolife\"")))
                .andExpect(content().string(containsString("https://app.sudolife.example/password-recovery/complete?token=raw-token-with%20secret")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        sender.send(new PasswordRecoveryEmail(EMAIL, TOKEN));

        server.verify();
    }

    @Test
    void send_throws_safe_exception_when_resend_rejects_request() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(API_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        ResendPasswordRecoveryMailSender sender = mailSender(restClientBuilder.build());
        server.expect(requestTo(API_URL + "/emails"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> sender.send(new PasswordRecoveryEmail(EMAIL, TOKEN)))
                .isInstanceOf(PasswordRecoveryMailDeliveryException.class)
                .hasMessage("Password Recovery mail delivery is temporarily unavailable")
                .hasNoCause()
                .message()
                .doesNotContain(TOKEN, API_KEY, API_URL);
        server.verify();
    }

    private ResendPasswordRecoveryMailSender mailSender(RestClient restClient) {
        return new ResendPasswordRecoveryMailSender(
                properties(),
                new PasswordRecoveryLinkBuilder(FRONTEND_BASE_URL),
                restClient
        );
    }

    private PasswordRecoveryMailProperties.Resend properties() {
        return new PasswordRecoveryMailProperties.Resend(
                API_KEY,
                SENDER,
                API_URL,
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)
        );
    }
}
