package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAccountLinkEntity;
import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
import com.sudolife.application.service.strava.exception.InvalidStravaAccountLinkStateException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.StravaTestHelper.UNLINKED_AT;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.pendingAuthorizationState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class StravaPersistenceAdapterIntegrationTest {

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private StravaAuthorizationStateRepository authorizationStateRepository;

    @Autowired
    private SpringDataStravaAccountLinkRepository springDataAccountLinkRepository;

    @Autowired
    private SpringDataStravaAuthorizationStateRepository springDataAuthorizationStateRepository;

    @BeforeEach
    void setUp() {
        springDataAuthorizationStateRepository.deleteAll();
        springDataAccountLinkRepository.deleteAll();
    }

    @Test
    void save_and_find_active_link_by_user_email() {
        StravaAccountLink link = activeLink(USER_EMAIL, ATHLETE_ID);

        accountLinkRepository.save(link);

        Optional<StravaAccountLink> foundLink = accountLinkRepository.findActiveByUserEmail(USER_EMAIL);
        assertThat(foundLink).isPresent();
        assertThat(foundLink.get().getAthleteId()).isEqualTo(ATHLETE_ID);
        assertThat(foundLink.get().getAccessToken()).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    void save_and_find_active_link_by_athlete_id() {
        StravaAccountLink link = activeLink(USER_EMAIL, ATHLETE_ID);

        accountLinkRepository.save(link);

        Optional<StravaAccountLink> foundLink = accountLinkRepository.findActiveByAthleteId(ATHLETE_ID);
        assertThat(foundLink).isPresent();
        assertThat(foundLink.get().getUserEmail()).isEqualTo(USER_EMAIL);
    }

    @Test
    void inactive_historical_records_remain_queryable_while_active_lookup_ignores_them() {
        accountLinkRepository.save(inactiveLink("former@sudolife.com", ATHLETE_ID));

        Optional<StravaAccountLink> activeLink = accountLinkRepository.findActiveByUserEmail("former@sudolife.com");

        List<StravaAccountLinkEntity> historicalLinks = springDataAccountLinkRepository.findByUserEmailOrderByLinkedAtAsc(
                "former@sudolife.com");
        assertThat(activeLink).isEmpty();
        assertThat(historicalLinks).hasSize(1);
        assertThat(historicalLinks.getFirst().isActive()).isFalse();
    }

    @Test
    void save_and_find_authorization_state_by_state_value() {
        StravaAuthorizationState authorizationState = pendingAuthorizationState();

        authorizationStateRepository.save(authorizationState);

        Optional<StravaAuthorizationState> foundState = authorizationStateRepository.findByState(STATE);
        assertThat(foundState).isPresent();
        assertThat(foundState.get().getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(foundState.get().getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void consume_pending_authorization_state_consumes_state_once() {
        authorizationStateRepository.save(pendingAuthorizationState());

        Optional<StravaAuthorizationState> consumedState = authorizationStateRepository.consumePending(STATE, NOW,
                NOW);

        Optional<StravaAuthorizationState> secondConsumption = authorizationStateRepository.consumePending(STATE, NOW,
                NOW.plusSeconds(1));
        assertThat(consumedState).isPresent();
        assertThat(consumedState.get().getConsumedAt()).isEqualTo(NOW);
        assertThat(secondConsumption).isEmpty();
    }

    @Test
    void consume_pending_authorization_state_rejects_expired_state() {
        authorizationStateRepository.save(pendingAuthorizationState());

        Optional<StravaAuthorizationState> consumedState = authorizationStateRepository.consumePending(STATE,
                EXPIRES_AT, EXPIRES_AT);

        assertThat(consumedState).isEmpty();
    }

    @Test
    void duplicate_active_athlete_is_rejected() {
        accountLinkRepository.save(activeLink("first@sudolife.com", ATHLETE_ID));

        assertThatThrownBy(() -> accountLinkRepository.save(activeLink("second@sudolife.com", ATHLETE_ID)))
                .isInstanceOf(DuplicateStravaAthleteOwnershipException.class);
    }

    @Test
    void duplicate_active_user_is_rejected() {
        accountLinkRepository.save(activeLink(USER_EMAIL, ATHLETE_ID));

        assertThatThrownBy(() -> accountLinkRepository.save(activeLink(USER_EMAIL, ATHLETE_ID + 1)))
                .isInstanceOf(InvalidStravaAccountLinkStateException.class);
    }

    @Test
    void inactive_history_does_not_block_new_active_link_for_same_athlete() {
        accountLinkRepository.save(inactiveLink("former@sudolife.com", ATHLETE_ID));

        StravaAccountLink savedLink = accountLinkRepository.save(activeLink("current@sudolife.com", ATHLETE_ID));

        assertThat(savedLink.isLinked()).isTrue();
        assertThat(savedLink.getAthleteId()).isEqualTo(ATHLETE_ID);
    }

    private StravaAccountLink activeLink(String userEmail, Long athleteId) {
        return StravaAccountLink.active(null, userEmail, athleteId, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, LINKED_AT);
    }

    private StravaAccountLink inactiveLink(String userEmail, Long athleteId) {
        return new StravaAccountLink(null, userEmail, athleteId, null, null, null, false, LINKED_AT, UNLINKED_AT);
    }
}
