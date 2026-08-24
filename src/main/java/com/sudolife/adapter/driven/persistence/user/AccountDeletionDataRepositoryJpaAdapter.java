package com.sudolife.adapter.driven.persistence.user;

import com.sudolife.adapter.driven.persistence.strava.consent.SpringDataStravaDataConsentRecordRepository;
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
import com.sudolife.application.service.user.ports.required.AccountDeletionDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountDeletionDataRepositoryJpaAdapter implements AccountDeletionDataRepository {

    private final SpringDataAdaptiveRunningPlanRepository adaptiveRunningPlanRepository;
    private final SpringDataCoachingProfileRepository coachingProfileRepository;
    private final SpringDataTrainingProfileRepository trainingProfileRepository;
    private final SpringDataStravaActivityStreamSyncJobRepository streamSyncJobRepository;
    private final SpringDataStravaSummarySyncJobRepository summarySyncJobRepository;
    private final SpringDataStravaActivityStreamSnapshotRepository streamSnapshotRepository;
    private final SpringDataStravaActivityDetailSnapshotRepository detailSnapshotRepository;
    private final SpringDataStravaActivitySummaryRepository activitySummaryRepository;
    private final SpringDataStravaAuthorizationStateRepository authorizationStateRepository;
    private final SpringDataStravaAccountLinkRepository accountLinkRepository;
    private final SpringDataStravaDataConsentRecordRepository consentRecordRepository;
    private final PasswordRecoveryTokenJpaRepository passwordRecoveryTokenRepository;

    @Override
    public List<StravaDeauthorization> findStravaDeauthorizations(String userEmail) {
        return accountLinkRepository.findByUserEmailOrderByLinkedAtAsc(userEmail).stream()
                .filter(link -> hasText(link.getAccessToken()) || hasText(link.getRefreshToken()))
                .map(this::toDeauthorization)
                .toList();
    }

    @Override
    public void deleteAccountOwnedData(String userEmail, Instant now) {
        List<Long> accountLinkIds = accountLinkRepository.findIdsByUserEmail(userEmail);
        cancelOpenStravaJobs(userEmail, now);
        streamSyncJobRepository.deleteByUserEmail(userEmail);
        summarySyncJobRepository.deleteByUserEmail(userEmail);
        streamSnapshotRepository.deleteByUserEmail(userEmail);
        detailSnapshotRepository.deleteByUserEmail(userEmail);
        activitySummaryRepository.deleteByUserEmail(userEmail);
        authorizationStateRepository.deleteByUserEmail(userEmail);
        deleteAccountLinks(accountLinkIds);
        consentRecordRepository.deleteByUserEmail(userEmail);
        adaptiveRunningPlanRepository.clearOriginalPlannedSessionReferencesByUserEmail(userEmail);
        adaptiveRunningPlanRepository.deleteSessionsByUserEmail(userEmail);
        adaptiveRunningPlanRepository.deleteByUserEmail(userEmail);
        coachingProfileRepository.deleteByUserEmail(userEmail);
        trainingProfileRepository.deleteByUserEmail(userEmail);
        passwordRecoveryTokenRepository.deleteByUserEmail(userEmail);
    }

    private void cancelOpenStravaJobs(String userEmail, Instant now) {
        streamSyncJobRepository.cancelOpenByUserEmail(userEmail, now);
        summarySyncJobRepository.cancelOpenByUserEmail(userEmail, now);
    }

    private void deleteAccountLinks(List<Long> accountLinkIds) {
        if (!accountLinkIds.isEmpty()) {
            accountLinkRepository.deleteByIdIn(accountLinkIds);
        }
    }

    private StravaDeauthorization toDeauthorization(StravaAccountLinkEntity link) {
        return new StravaDeauthorization(link.getAthleteId(), link.getAccessToken(), link.getRefreshToken(),
                link.getExpiresAt());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
