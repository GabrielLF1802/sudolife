package com.sudolife.adapter.driven.persistence.user;

import com.sudolife.adapter.driven.persistence.strava.activity.SpringDataStravaActivityDetailSnapshotRepository;
import com.sudolife.adapter.driven.persistence.strava.activity.SpringDataStravaActivityStreamSnapshotRepository;
import com.sudolife.adapter.driven.persistence.strava.activity.SpringDataStravaActivitySummaryRepository;
import com.sudolife.adapter.driven.persistence.strava.linking.SpringDataStravaAccountLinkRepository;
import com.sudolife.adapter.driven.persistence.strava.linking.SpringDataStravaAuthorizationStateRepository;
import com.sudolife.adapter.driven.persistence.strava.linking.entitymodel.StravaAccountLinkEntity;
import com.sudolife.adapter.driven.persistence.strava.sync.SpringDataStravaActivityStreamSyncJobRepository;
import com.sudolife.adapter.driven.persistence.strava.sync.SpringDataStravaSummarySyncJobRepository;
import com.sudolife.adapter.driven.persistence.training.SpringDataTrainingProfileRepository;
import com.sudolife.adapter.driven.persistence.training.coaching.SpringDataCoachingProfileRepository;
import com.sudolife.adapter.driven.persistence.training.plan.SpringDataAdaptiveRunningPlanRepository;
import com.sudolife.application.service.user.StravaDeauthorization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionDataRepositoryJpaAdapterUnitTest {

    private static final Long ACCOUNT_LINK_ID = 200L;

    @Mock
    private SpringDataAdaptiveRunningPlanRepository adaptiveRunningPlanRepository;

    @Mock
    private SpringDataCoachingProfileRepository coachingProfileRepository;

    @Mock
    private SpringDataTrainingProfileRepository trainingProfileRepository;

    @Mock
    private SpringDataStravaActivityStreamSyncJobRepository streamSyncJobRepository;

    @Mock
    private SpringDataStravaSummarySyncJobRepository summarySyncJobRepository;

    @Mock
    private SpringDataStravaActivityStreamSnapshotRepository streamSnapshotRepository;

    @Mock
    private SpringDataStravaActivityDetailSnapshotRepository detailSnapshotRepository;

    @Mock
    private SpringDataStravaActivitySummaryRepository activitySummaryRepository;

    @Mock
    private SpringDataStravaAuthorizationStateRepository authorizationStateRepository;

    @Mock
    private SpringDataStravaAccountLinkRepository accountLinkRepository;

    @InjectMocks
    private AccountDeletionDataRepositoryJpaAdapter repository;

    @Test
    void delete_account_owned_data_cancels_open_jobs_before_deleting_rows_in_constraint_order() {
        when(accountLinkRepository.findIdsByUserEmail(USER_EMAIL)).thenReturn(List.of(ACCOUNT_LINK_ID));

        repository.deleteAccountOwnedData(USER_EMAIL, NOW);

        InOrder cleanupOrder = inOrder(streamSyncJobRepository, summarySyncJobRepository, streamSnapshotRepository,
                detailSnapshotRepository, activitySummaryRepository, authorizationStateRepository,
                accountLinkRepository, adaptiveRunningPlanRepository, coachingProfileRepository,
                trainingProfileRepository);
        cleanupOrder.verify(streamSyncJobRepository).cancelOpenByUserEmail(USER_EMAIL, NOW);
        cleanupOrder.verify(summarySyncJobRepository).cancelOpenByUserEmail(USER_EMAIL, NOW);
        cleanupOrder.verify(streamSyncJobRepository).deleteByUserEmail(USER_EMAIL);
        cleanupOrder.verify(summarySyncJobRepository).deleteByUserEmail(USER_EMAIL);
        cleanupOrder.verify(streamSnapshotRepository).deleteByUserEmail(USER_EMAIL);
        cleanupOrder.verify(detailSnapshotRepository).deleteByUserEmail(USER_EMAIL);
        cleanupOrder.verify(activitySummaryRepository).deleteByUserEmail(USER_EMAIL);
        cleanupOrder.verify(authorizationStateRepository).deleteByUserEmail(USER_EMAIL);
        cleanupOrder.verify(accountLinkRepository).deleteByIdIn(List.of(ACCOUNT_LINK_ID));
        cleanupOrder.verify(adaptiveRunningPlanRepository).clearOriginalPlannedSessionReferencesByUserEmail(USER_EMAIL);
        cleanupOrder.verify(adaptiveRunningPlanRepository).deleteSessionsByUserEmail(USER_EMAIL);
        cleanupOrder.verify(adaptiveRunningPlanRepository).deleteByUserEmail(USER_EMAIL);
        cleanupOrder.verify(coachingProfileRepository).deleteByUserEmail(USER_EMAIL);
        cleanupOrder.verify(trainingProfileRepository).deleteByUserEmail(USER_EMAIL);
    }

    @Test
    void find_strava_deauthorizations_returns_links_with_usable_token_material() {
        when(accountLinkRepository.findByUserEmailOrderByLinkedAtAsc(USER_EMAIL)).thenReturn(List.of(
                accountLink(ACCESS_TOKEN, REFRESH_TOKEN),
                accountLink(null, null)
        ));

        List<StravaDeauthorization> deauthorizations = repository.findStravaDeauthorizations(USER_EMAIL);

        assertThat(deauthorizations).containsExactly(new StravaDeauthorization(ATHLETE_ID, ACCESS_TOKEN,
                REFRESH_TOKEN, EXPIRES_AT));
    }

    private StravaAccountLinkEntity accountLink(String accessToken, String refreshToken) {
        StravaAccountLinkEntity link = new StravaAccountLinkEntity();
        link.setAthleteId(ATHLETE_ID);
        link.setAccessToken(accessToken);
        link.setRefreshToken(refreshToken);
        link.setExpiresAt(EXPIRES_AT);

        return link;
    }
}
