package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAccountLinkEntity;
import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.StravaActivitySummaryPage;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
import com.sudolife.application.service.strava.exception.InvalidStravaAccountLinkStateException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.StravaTestHelper.UNLINKED_AT;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.pendingAuthorizationState;
import static com.sudolife.helper.StravaTestHelper.stravaActivitySummary;
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
    private StravaActivitySummaryRepository activitySummaryRepository;

    @Autowired
    private SpringDataStravaAccountLinkRepository springDataAccountLinkRepository;

    @Autowired
    private SpringDataStravaActivitySummaryRepository springDataActivitySummaryRepository;

    @Autowired
    private SpringDataStravaAuthorizationStateRepository springDataAuthorizationStateRepository;

    @BeforeEach
    void setUp() {
        springDataAuthorizationStateRepository.deleteAll();
        springDataActivitySummaryRepository.deleteAll();
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

    @Test
    void save_activity_summary_once_per_user_and_source_activity() {
        StravaActivitySummary summary = stravaActivitySummary();

        boolean firstSave = activitySummaryRepository.saveIfAbsent(summary);
        boolean secondSave = activitySummaryRepository.saveIfAbsent(summary);

        assertThat(firstSave).isTrue();
        assertThat(secondSave).isFalse();
        assertThat(activitySummaryRepository.countByUserEmail(USER_EMAIL)).isEqualTo(1);
        assertThat(springDataActivitySummaryRepository.findAll()).hasSize(1);
        assertThat(springDataActivitySummaryRepository.findAll().getFirst().getName()).isEqualTo("Morning Run");
    }

    @Test
    void same_source_activity_can_be_imported_for_different_users() {
        activitySummaryRepository.saveIfAbsent(stravaActivitySummary());
        StravaActivitySummary otherUserSummary = activitySummaryForUser("other@sudolife.com");

        boolean saved = activitySummaryRepository.saveIfAbsent(otherUserSummary);

        assertThat(saved).isTrue();
        assertThat(activitySummaryRepository.countByUserEmail(USER_EMAIL)).isEqualTo(1);
        assertThat(activitySummaryRepository.countByUserEmail("other@sudolife.com")).isEqualTo(1);
    }

    @Test
    void list_activity_summaries_filters_by_owner_and_sorts_newest_first() {
        activitySummaryRepository.saveIfAbsent(activitySummary("older@sudolife.com", SOURCE_ACTIVITY_ID + 3,
                "Other User Run", "2026-05-12T09:00:00Z"));
        activitySummaryRepository.saveIfAbsent(activitySummary(USER_EMAIL, SOURCE_ACTIVITY_ID + 1,
                "Older Run", "2026-05-09T09:00:00Z"));
        activitySummaryRepository.saveIfAbsent(activitySummary(USER_EMAIL, SOURCE_ACTIVITY_ID + 2,
                "Newest Run", "2026-05-11T09:00:00Z"));

        StravaActivitySummaryPage result = activitySummaryRepository.findByUserEmail(USER_EMAIL, 0, 10);

        assertThat(result.activities()).extracting(StravaActivitySummary::getName)
                .containsExactly("Newest Run", "Older Run");
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void list_activity_summaries_supports_page_and_size() {
        activitySummaryRepository.saveIfAbsent(activitySummary(USER_EMAIL, SOURCE_ACTIVITY_ID + 1,
                "Oldest Run", "2026-05-08T09:00:00Z"));
        activitySummaryRepository.saveIfAbsent(activitySummary(USER_EMAIL, SOURCE_ACTIVITY_ID + 2,
                "Middle Run", "2026-05-09T09:00:00Z"));
        activitySummaryRepository.saveIfAbsent(activitySummary(USER_EMAIL, SOURCE_ACTIVITY_ID + 3,
                "Newest Run", "2026-05-10T09:00:00Z"));

        StravaActivitySummaryPage result = activitySummaryRepository.findByUserEmail(USER_EMAIL, 1, 1);

        assertThat(result.activities()).extracting(StravaActivitySummary::getName).containsExactly("Middle Run");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    private StravaActivitySummary activitySummaryForUser(String userEmail) {
        StravaActivitySummary summary = stravaActivitySummary();

        return StravaActivitySummary.imported(userEmail, LINK_ID, summary.getSourceActivityId(),
                summary.getActivityType(), summary.getRawSportType(), summary.getName(), summary.getStartDate(),
                summary.getDistanceMeters(), summary.getMovingTimeSeconds(), summary.getAverageSpeedMetersPerSecond(),
                summary.getTotalElevationGainMeters(), summary.getMaxSpeedMetersPerSecond(),
                summary.getAverageHeartRate(), summary.getMaxHeartRate(), summary.getAverageCadence(),
                summary.getAverageWatts(), summary.getCalories(), NOW);
    }

    private StravaActivitySummary activitySummary(String userEmail, Long sourceActivityId, String name,
                                                  String startDate) {
        return StravaActivitySummary.imported(userEmail, LINK_ID, sourceActivityId, StravaActivityType.RUN, "Run",
                name, Instant.parse(startDate), 5000.0, 1500, 3.33, 42.0, 5.5, 150.0, 180.0,
                82.0, 220.0, 350.0, NOW);
    }

    private StravaAccountLink activeLink(String userEmail, Long athleteId) {
        return StravaAccountLink.active(null, userEmail, athleteId, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, LINKED_AT);
    }

    private StravaAccountLink inactiveLink(String userEmail, Long athleteId) {
        return new StravaAccountLink(null, userEmail, athleteId, null, null, null, false, LINKED_AT, UNLINKED_AT);
    }
}
