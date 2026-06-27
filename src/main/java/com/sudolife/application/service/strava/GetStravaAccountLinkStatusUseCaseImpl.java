package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import com.sudolife.application.service.strava.ports.provided.GetStravaAccountLinkStatusUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GetStravaAccountLinkStatusUseCaseImpl implements GetStravaAccountLinkStatusUseCase {

    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaSummarySyncJobRepository summarySyncJobRepository;
    private final StravaActivitySummaryRepository activitySummaryRepository;

    @Override
    public StravaLinkStatusResult execute(GetStravaAccountLinkStatusCommand command) {
        return accountLinkRepository.findActiveByUserEmail(command.userEmail())
                .map(this::linkedStatus)
                .orElseGet(this::unlinkedStatus);
    }

    private StravaLinkStatusResult linkedStatus(StravaAccountLink accountLink) {
        if (!accountLink.hasActivityReadScope()) {
            return new StravaLinkStatusResult(true, accountLink.getAthleteId(),
                    StravaPermissionState.PERMISSION_UPGRADE_REQUIRED,
                    StravaSummaryStatus.PERMISSION_UPGRADE_REQUIRED,
                    StravaPerformanceDataStatus.PERMISSION_UPGRADE_REQUIRED, null, null, 0, 0,
                    StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED);
        }

        long importedActivityCount = activitySummaryRepository.countByAccountLinkId(accountLink.getId());
        long streamsReadyActivityCount = activitySummaryRepository.countStreamsReadyByAccountLinkId(accountLink.getId());
        Instant lastSummarySyncTime = lastSummarySyncTime(accountLink.getId());

        return summarySyncJobRepository.findLatestByAccountLinkId(accountLink.getId())
                .map(job -> syncedStatus(accountLink, job, lastSummarySyncTime, importedActivityCount,
                        streamsReadyActivityCount))
                .orElseGet(() -> notStartedStatus(accountLink, importedActivityCount, streamsReadyActivityCount));
    }

    private StravaLinkStatusResult unlinkedStatus() {
        return new StravaLinkStatusResult(false, null, StravaPermissionState.UNLINKED, StravaSummaryStatus.UNLINKED,
                StravaPerformanceDataStatus.UNLINKED, null, null, 0, 0, null);
    }

    private StravaLinkStatusResult notStartedStatus(StravaAccountLink accountLink, long importedActivityCount,
                                                    long streamsReadyActivityCount) {
        return new StravaLinkStatusResult(true, accountLink.getAthleteId(), StravaPermissionState.READY,
                StravaSummaryStatus.NOT_STARTED, performanceDataStatus(importedActivityCount,
                streamsReadyActivityCount), null, null, importedActivityCount, streamsReadyActivityCount, null);
    }

    private StravaLinkStatusResult syncedStatus(StravaAccountLink accountLink, StravaSummarySyncJob job,
                                                Instant lastSummarySyncTime, long importedActivityCount,
                                                long streamsReadyActivityCount) {
        return new StravaLinkStatusResult(true, accountLink.getAthleteId(), StravaPermissionState.READY,
                activitySummaryStatus(job), performanceDataStatus(importedActivityCount, streamsReadyActivityCount),
                lastSummarySyncTime, null, importedActivityCount, streamsReadyActivityCount, failureReason(job));
    }

    private StravaSummaryStatus activitySummaryStatus(StravaSummarySyncJob job) {
        if (job.getStatus() == StravaSummarySyncJobStatus.QUEUED) {
            return StravaSummaryStatus.QUEUED;
        }

        if (job.getStatus() == StravaSummarySyncJobStatus.RUNNING) {
            return StravaSummaryStatus.RUNNING;
        }

        if (job.getStatus() == StravaSummarySyncJobStatus.COMPLETED) {
            return StravaSummaryStatus.COMPLETED;
        }

        return StravaSummaryStatus.FAILED;
    }

    private StravaPerformanceDataStatus performanceDataStatus(long importedActivityCount,
                                                              long streamsReadyActivityCount) {
        if (importedActivityCount == 0) {
            return StravaPerformanceDataStatus.NOT_STARTED;
        }

        if (streamsReadyActivityCount >= importedActivityCount) {
            return StravaPerformanceDataStatus.READY;
        }

        return StravaPerformanceDataStatus.PENDING;
    }

    private Instant lastSummarySyncTime(Long accountLinkId) {
        return summarySyncJobRepository.findLatestCompletedByAccountLinkId(accountLinkId)
                .map(StravaSummarySyncJob::getCompletedAt)
                .orElse(null);
    }

    private StravaActivitySyncFailureReason failureReason(StravaSummarySyncJob job) {
        if (job.getStatus() == StravaSummarySyncJobStatus.QUEUED ||
                job.getStatus() == StravaSummarySyncJobStatus.RUNNING) {
            return StravaActivitySyncFailureReason.SYNC_ALREADY_RUNNING;
        }

        if (job.getStatus() != StravaSummarySyncJobStatus.FAILED) {
            return null;
        }

        try {
            return StravaActivitySyncFailureReason.valueOf(job.getFailureReason());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return StravaActivitySyncFailureReason.UNKNOWN_SYNC_FAILURE;
        }
    }
}
