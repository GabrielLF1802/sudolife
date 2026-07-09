package com.sudolife.application.service.strava.linking;

import com.sudolife.application.service.strava.authorization.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.authorization.StravaTokenAuthorization;
import com.sudolife.adapter.driven.persistence.strava.linking.SpringDataStravaAccountLinkRepository;
import com.sudolife.adapter.driven.persistence.strava.linking.entitymodel.StravaAccountLinkEntity;
import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.ports.provided.UnlinkStravaAccountUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
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
import java.util.List;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.unlinkStravaAccountCommand;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(FixedTimeProvider.class)
@Transactional
class UnlinkStravaAccountUseCaseImplIntegrationTest {

    private static final String ROTATED_ACCESS_TOKEN = "rotated-access-token";
    private static final String ROTATED_REFRESH_TOKEN = "rotated-refresh-token";
    private static final Instant EXPIRED_AT = Instant.parse("2026-05-11T11:00:00Z");

    @Autowired
    private UnlinkStravaAccountUseCase useCase;

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private SpringDataStravaAccountLinkRepository springDataRepository;

    @Autowired
    private FakeStravaOAuthProvider oAuthProvider;

    @BeforeEach
    void setUp() {
        oAuthProvider.clear();
    }

    @Test
    void execute_unlinks_active_account_with_real_repository_and_retains_history() {
        accountLinkRepository.save(StravaAccountLink.active(null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRES_AT, LINKED_AT));

        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        List<StravaAccountLinkEntity> history = springDataRepository.findByUserEmailOrderByLinkedAtAsc(USER_EMAIL);
        assertThat(result.unlinked()).isTrue();
        assertThat(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).isEmpty();
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().isActive()).isFalse();
        assertThat(history.getFirst().getUnlinkedAt()).isEqualTo(NOW);
        assertThat(history.getFirst().getAccessToken()).isNull();
        assertThat(history.getFirst().getRefreshToken()).isNull();
        assertThat(oAuthProvider.deauthorizedAccessToken()).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    void execute_without_active_account_is_successful_and_changes_nothing() {
        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        assertThat(result.unlinked()).isTrue();
        assertThat(springDataRepository.findByUserEmailOrderByLinkedAtAsc(USER_EMAIL)).isEmpty();
        assertThat(oAuthProvider.deauthorizedAccessToken()).isNull();
    }

    @Test
    void execute_refreshes_expired_token_before_deauthorization() {
        accountLinkRepository.save(StravaAccountLink.active(null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRED_AT, LINKED_AT));
        oAuthProvider.authorizeRefresh(new StravaTokenAuthorization(ATHLETE_ID, ROTATED_ACCESS_TOKEN,
                ROTATED_REFRESH_TOKEN, EXPIRES_AT, SCOPE));

        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        assertThat(result.unlinked()).isTrue();
        assertThat(oAuthProvider.refreshedToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(oAuthProvider.deauthorizedAccessToken()).isEqualTo(ROTATED_ACCESS_TOKEN);
        assertThat(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).isEmpty();
    }

    @Test
    void execute_when_deauthorization_fails_still_marks_link_inactive() {
        accountLinkRepository.save(StravaAccountLink.active(null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRES_AT, LINKED_AT));
        oAuthProvider.failDeauthorization();

        UnlinkStravaAccountResult result = useCase.execute(unlinkStravaAccountCommand());

        assertThat(result.unlinked()).isTrue();
        assertThat(accountLinkRepository.findActiveByUserEmail(USER_EMAIL)).isEmpty();
    }

    @TestConfiguration
    static class UnlinkStravaAccountUseCaseImplIntegrationTestConfig {

        @Bean
        @Primary
        FakeStravaOAuthProvider fakeStravaOAuthProvider() {
            return new FakeStravaOAuthProvider();
        }
    }

    static class FakeStravaOAuthProvider implements StravaOAuthProvider {

        private StravaTokenAuthorization refreshAuthorization;
        private String refreshedToken;
        private String deauthorizedAccessToken;
        private boolean deauthorizationFailure;

        @Override
        public String buildAuthorizationUrl(StravaAuthorizationRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StravaTokenAuthorization exchangeAuthorizationCode(String code) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StravaTokenAuthorization refresh(String refreshToken) {
            refreshedToken = refreshToken;

            return refreshAuthorization;
        }

        @Override
        public void deauthorize(String accessToken) {
            if (deauthorizationFailure) {
                throw new RuntimeException("deauthorization failed");
            }

            deauthorizedAccessToken = accessToken;
        }

        void authorizeRefresh(StravaTokenAuthorization refreshAuthorization) {
            this.refreshAuthorization = refreshAuthorization;
        }

        void failDeauthorization() {
            deauthorizationFailure = true;
        }

        String refreshedToken() {
            return refreshedToken;
        }

        String deauthorizedAccessToken() {
            return deauthorizedAccessToken;
        }

        void clear() {
            refreshAuthorization = null;
            refreshedToken = null;
            deauthorizedAccessToken = null;
            deauthorizationFailure = false;
        }
    }
}
