package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.ports.provided.StartStravaAccountLinkingUseCase;
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

import java.time.Instant;
import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.startStravaAccountLinkingCommand;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(FixedTimeProvider.class)
@Transactional
class StartStravaAccountLinkingUseCaseImplIntegrationTest {

    private static final String AUTHORIZATION_URL = "https://www.strava.com/oauth/authorize?scope=read";
    private static final Instant EXPECTED_EXPIRATION = Instant.parse("2026-05-11T12:10:00Z");

    @Autowired
    private StartStravaAccountLinkingUseCase useCase;

    @Autowired
    private StravaAuthorizationStateRepository authorizationStateRepository;

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private FakeStravaOAuthProvider oAuthProvider;

    @BeforeEach
    void setUp() {
        oAuthProvider.clear();
    }

    @Test
    void execute_persists_state_and_returns_provider_authorization_url_without_linking_account() {
        StartStravaAccountLinkingCommand command = startStravaAccountLinkingCommand();

        StravaAuthorizationUrlResult result = useCase.execute(command);

        Optional<StravaAuthorizationState> savedState = authorizationStateRepository.findByState(oAuthProvider.lastRequest().state());
        assertThat(result.authorizationUrl()).isEqualTo(AUTHORIZATION_URL);
        assertThat(savedState).isPresent();
        assertThat(savedState.get().getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(savedState.get().getExpiresAt()).isEqualTo(EXPECTED_EXPIRATION);
        assertThat(oAuthProvider.lastRequest().scope()).isEqualTo("read");
        assertThat(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).isEmpty();
    }

    @TestConfiguration
    static class StartStravaAccountLinkingUseCaseImplIntegrationTestConfig {

        @Bean
        @Primary
        FakeStravaOAuthProvider fakeStravaOAuthProvider() {
            return new FakeStravaOAuthProvider();
        }

    }

    static class FakeStravaOAuthProvider implements StravaOAuthProvider {

        private StravaAuthorizationRequest lastRequest;

        @Override
        public String buildAuthorizationUrl(StravaAuthorizationRequest request) {
            lastRequest = request;

            return AUTHORIZATION_URL;
        }

        @Override
        public StravaTokenAuthorization exchangeAuthorizationCode(String code) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StravaTokenAuthorization refresh(String refreshToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deauthorize(String accessToken) {
            throw new UnsupportedOperationException();
        }

        StravaAuthorizationRequest lastRequest() {
            return lastRequest;
        }

        void clear() {
            lastRequest = null;
        }
    }

}
