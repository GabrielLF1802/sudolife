package com.sudolife.adapter.driven.mail;

import com.sudolife.application.service.user.PasswordRecoveryEmail;
import com.sudolife.application.service.user.exception.PasswordRecoveryMailDeliveryException;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "password-recovery.mail.delivery", havingValue = "resend")
public class ResendPasswordRecoveryMailSender implements PasswordRecoveryMailSender {

    private static final String SUBJECT = "Recupere sua senha no Sudolife";

    private final PasswordRecoveryMailProperties.Resend properties;
    private final PasswordRecoveryLinkBuilder linkBuilder;
    private final RestClient restClient;

    @Autowired
    public ResendPasswordRecoveryMailSender(
            PasswordRecoveryMailProperties properties,
            PasswordRecoveryLinkBuilder linkBuilder
    ) {
        this(properties.resend(), linkBuilder, RestClient.builder()
                .baseUrl(properties.resend().apiUrl())
                .requestFactory(requestFactory(properties.resend()))
                .build());
    }

    ResendPasswordRecoveryMailSender(
            PasswordRecoveryMailProperties.Resend properties,
            PasswordRecoveryLinkBuilder linkBuilder,
            RestClient restClient
    ) {
        properties.validateProductionConfiguration();
        this.properties = properties;
        this.linkBuilder = linkBuilder;
        this.restClient = restClient;
    }

    @Override
    public void send(PasswordRecoveryEmail email) {
        try {
            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request(email))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.warn("Resend password recovery mail request failed statusCode={}", response.getStatusCode().value());
                        throw new PasswordRecoveryMailDeliveryException();
                    })
                    .toBodilessEntity();
        } catch (PasswordRecoveryMailDeliveryException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Resend password recovery mail request failed category=client_error");
            throw new PasswordRecoveryMailDeliveryException(exception);
        }
    }

    private ResendEmailRequest request(PasswordRecoveryEmail email) {
        String recoveryLink = linkBuilder.build(email);

        return new ResendEmailRequest(
                properties.sender(),
                List.of(email.email()),
                SUBJECT,
                html(recoveryLink)
        );
    }

    private String html(String recoveryLink) {
        return "<p>Recebemos uma solicitação para redefinir sua senha no Sudolife.</p>"
                + "<p><a href=\"" + recoveryLink + "\">Redefinir senha</a></p>"
                + "<p>Se você não solicitou essa alteração, ignore este email.</p>";
    }

    private static SimpleClientHttpRequestFactory requestFactory(PasswordRecoveryMailProperties.Resend properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return requestFactory;
    }

    private record ResendEmailRequest(String from, List<String> to, String subject, String html) {
    }
}
