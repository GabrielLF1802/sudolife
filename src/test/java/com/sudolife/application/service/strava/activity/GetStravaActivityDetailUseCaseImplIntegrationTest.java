package com.sudolife.application.service.strava.activity;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivityDetailImport;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.exception.StravaActivityNotFoundException;
import com.sudolife.application.service.strava.ports.provided.GetStravaActivityDetailUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityDetailSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
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
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.stravaActivitySummary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
@Import({FixedTimeProvider.class, GetStravaActivityDetailUseCaseImplIntegrationTest.TestConfig.class})
@Transactional
class GetStravaActivityDetailUseCaseImplIntegrationTest {

    @Autowired
    private GetStravaActivityDetailUseCase useCase;

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private StravaActivitySummaryRepository activitySummaryRepository;

    @Autowired
    private StravaActivityDetailSnapshotRepository detailSnapshotRepository;

    @Autowired
    private FakeStravaActivityProvider activityProvider;

    @BeforeEach
    void setUp() {
        activityProvider.reset();
    }

    @Test
    void execute_fetches_and_persists_missing_detail_snapshot_once() {
        Long activityId = importedActivityId();

        StravaActivityDetailResult firstResult = useCase.execute(command(activityId));
        StravaActivityDetailResult secondResult = useCase.execute(command(activityId));

        assertThat(firstResult.name()).isEqualTo("Morning Run Detail");
        assertThat(firstResult.enrichmentStatus()).isEqualTo(StravaActivityDetailEnrichmentStatus.COMPLETED);
        assertThat(secondResult.name()).isEqualTo("Morning Run Detail");
        assertThat(activityProvider.detailRequestCount()).isEqualTo(1);
        assertThat(detailSnapshotRepository.findByActivitySummaryId(activityId)).isPresent();
    }

    @Test
    void execute_returns_not_found_when_activity_is_not_owned_by_user() {
        Long activityId = importedActivityId();

        assertThatThrownBy(() -> useCase.execute(new GetStravaActivityDetailCommand("other@sudolife.com", activityId)))
                .isInstanceOf(StravaActivityNotFoundException.class);
    }

    private Long importedActivityId() {
        accountLinkRepository.save(StravaAccountLink.active(null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRES_AT, LINKED_AT));
        activitySummaryRepository.saveIfAbsent(stravaActivitySummary());

        return activitySummaryRepository.findByIdAndUserEmail(1L, USER_EMAIL)
                .orElseGet(() -> activitySummaryRepository.findByUserEmail(USER_EMAIL, 0, 1).activities().getFirst())
                .getId();
    }

    private GetStravaActivityDetailCommand command(Long activityId) {
        return new GetStravaActivityDetailCommand(USER_EMAIL, activityId);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeStravaActivityProvider fakeStravaActivityProvider() {
            return new FakeStravaActivityProvider();
        }
    }

    static class FakeStravaActivityProvider implements StravaActivityProvider {

        private int detailRequestCount;

        @Override
        public List<StravaActivitySummaryImport> fetchActivitySummaries(String accessToken, Instant after,
                                                                        Instant before) {
            return List.of();
        }

        @Override
        public StravaActivityDetailImport fetchActivityDetail(String accessToken, Long sourceActivityId) {
            if (!ACCESS_TOKEN.equals(accessToken) || !SOURCE_ACTIVITY_ID.equals(sourceActivityId)) {
                throw new IllegalArgumentException("Unexpected Strava detail request");
            }

            detailRequestCount++;

            return new StravaActivityDetailImport(sourceActivityId, StravaActivityType.RUN, "Run",
                    "Morning Run Detail", Instant.parse("2026-05-10T09:00:00Z"), 5100.0, 1510, 3.37,
                    43.0, 5.6, 151.0, 181.0, 83.0, 221.0, 351.0);
        }

        int detailRequestCount() {
            return detailRequestCount;
        }

        void reset() {
            detailRequestCount = 0;
        }
    }
}
