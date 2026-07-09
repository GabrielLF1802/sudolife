package com.sudolife.application.service.strava.linking;

import com.sudolife.application.service.strava.authorization.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.authorization.StravaTokenAuthorization;
import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.ports.provided.CompleteStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.CODE;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.completeStravaAccountLinkingCommand;
import static com.sudolife.helper.StravaTestHelper.pendingAuthorizationState;
import static com.sudolife.helper.StravaTestHelper.stravaTokenAuthorization;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(FixedTimeProvider.class)
@Transactional
class CompleteStravaAccountLinkingUseCaseImplIntegrationTest {

    private static final String OTHER_USER_EMAIL = "other@sudolife.com";
    private static final String ROTATED_ACCESS_TOKEN = "rotated-access-token";
    private static final String ROTATED_REFRESH_TOKEN = "rotated-refresh-token";

    @Autowired
    private CompleteStravaAccountLinkingUseCase useCase;

    @Autowired
    private StravaAuthorizationStateRepository authorizationStateRepository;

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private FakeStravaOAuthProvider oAuthProvider;

    @BeforeEach
    void setUp() {
        oAuthProvider.authorize(stravaTokenAuthorization());
    }

    @Test
    void execute_completes_linking_with_real_repositories() {
        authorizationStateRepository.save(pendingAuthorizationState());

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        Optional<StravaAccountLink> savedLink = accountLinkRepository.findActiveByUserEmail(USER_EMAIL);
        Optional<StravaAuthorizationState> savedState = authorizationStateRepository.findByState(STATE);
        assertThat(result.linked()).isTrue();
        assertThat(savedLink).isPresent();
        assertThat(savedLink.get().getAthleteId()).isEqualTo(ATHLETE_ID);
        assertThat(savedLink.get().getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(savedLink.get().getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(savedState).isPresent();
        assertThat(savedState.get().getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void execute_rejects_invalid_state_without_creating_link() {
        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INVALID_STATE");
        assertThat(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).isEmpty();
    }

    @Test
    void execute_rejects_denied_authorization_and_consumes_state() {
        authorizationStateRepository.save(pendingAuthorizationState());
        CompleteStravaAccountLinkingCommand command = new CompleteStravaAccountLinkingCommand(STATE, null, null,
                "access_denied");

        StravaCallbackResult result = useCase.execute(command);

        Optional<StravaAuthorizationState> savedState = authorizationStateRepository.findByState(STATE);
        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("AUTHORIZATION_DENIED");
        assertThat(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).isEmpty();
        assertThat(savedState).isPresent();
        assertThat(savedState.get().getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void execute_rejects_insufficient_scope_without_creating_link() {
        authorizationStateRepository.save(pendingAuthorizationState());
        oAuthProvider.authorize(new StravaTokenAuthorization(ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT,
                "profile:read_all"));

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INSUFFICIENT_SCOPE");
        assertThat(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).isEmpty();
    }

    @Test
    void execute_rejects_athlete_owned_by_another_user() {
        authorizationStateRepository.save(pendingAuthorizationState());
        accountLinkRepository.save(StravaAccountLink.active(null, OTHER_USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN,
                REFRESH_TOKEN, EXPIRES_AT, LINKED_AT));

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.failureCode()).isEqualTo("ATHLETE_ALREADY_LINKED");
        assertThat(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).isEmpty();
    }

    @Test
    void execute_reconnects_same_user_and_replaces_token_metadata() {
        authorizationStateRepository.save(pendingAuthorizationState());
        StravaAccountLink existingLink = accountLinkRepository.save(StravaAccountLink.active(null, USER_EMAIL,
                ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, LINKED_AT));
        oAuthProvider.authorize(new StravaTokenAuthorization(ATHLETE_ID, ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN,
                EXPIRES_AT.plusSeconds(60), SCOPE));

        StravaCallbackResult result = useCase.execute(completeStravaAccountLinkingCommand());

        Optional<StravaAccountLink> savedLink = accountLinkRepository.findActiveByUserEmail(USER_EMAIL);
        assertThat(result.linked()).isTrue();
        assertThat(savedLink).isPresent();
        assertThat(savedLink.get().getId()).isEqualTo(existingLink.getId());
        assertThat(savedLink.get().getAccessToken()).isEqualTo(ROTATED_ACCESS_TOKEN);
        assertThat(savedLink.get().getRefreshToken()).isEqualTo(ROTATED_REFRESH_TOKEN);
    }

    @TestConfiguration
    static class CompleteStravaAccountLinkingUseCaseImplIntegrationTestConfig {

        @Bean
        @Primary
        FakeStravaOAuthProvider fakeStravaOAuthProvider() {
            return new FakeStravaOAuthProvider();
        }
    }

    static class FakeStravaOAuthProvider implements StravaOAuthProvider {

        private StravaTokenAuthorization tokenAuthorization;

        @Override
        public String buildAuthorizationUrl(StravaAuthorizationRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StravaTokenAuthorization exchangeAuthorizationCode(String code) {
            if (!CODE.equals(code)) {
                throw new IllegalArgumentException("Invalid code");
            }

            return tokenAuthorization;
        }

        @Override
        public StravaTokenAuthorization refresh(String refreshToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deauthorize(String accessToken) {
            throw new UnsupportedOperationException();
        }

        void authorize(StravaTokenAuthorization tokenAuthorization) {
            this.tokenAuthorization = tokenAuthorization;
        }
    }
}
