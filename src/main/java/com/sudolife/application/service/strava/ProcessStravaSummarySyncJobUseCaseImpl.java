package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.exception.StravaReconnectRequiredException;
import com.sudolife.application.service.strava.ports.provided.ProcessStravaSummarySyncJobUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSyncJobRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessStravaSummarySyncJobUseCaseImpl implements ProcessStravaSummarySyncJobUseCase {

    private static final long INITIAL_SYNC_MONTHS = 7L;

    private final StravaSummarySyncJobRepository summarySyncJobRepository;
    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaActivityProvider activityProvider;
    private final StravaActivitySummaryRepository activitySummaryRepository;
    private final StravaActivityStreamSyncJobRepository streamSyncJobRepository;
    private final TimeProvider timeProvider;
    private final TransactionTemplate transactionTemplate;
    private final StravaAccessTokenService accessTokenService;
    private final StravaActivityStreamEligibility streamEligibility = new StravaActivityStreamEligibility();
    @Value("${strava.summary-sync.max-attempts:3}")
    private int maxAttempts;
    @Value("${strava.summary-sync.retry-backoff:PT15M}")
    private Duration retryBackoff;

    @Override
    public void execute(ProcessStravaSummarySyncJobCommand command) {
        Optional<StravaSummarySyncJob> pendingJob = summarySyncJobRepository.findById(command.jobId());

        if (pendingJob.isEmpty() || !pendingJob.get().isQueuedOrRunning()) {
            return;
        }

        StravaSummarySyncJob runningJob = markRunning(pendingJob.get());
        accountLinkRepository.findActiveById(runningJob.getAccountLinkId())
                .filter(StravaAccountLink::canSyncActivities)
                .ifPresentOrElse(accountLink -> sync(runningJob, accountLink), () -> markPermanentFailure(runningJob,
                        disabledFailureReason(runningJob.getAccountLinkId())));
    }

    private StravaSummarySyncJob markRunning(StravaSummarySyncJob job) {
        StravaSummarySyncJob runningJob = summarySyncJobRepository.save(job.running(timeProvider.now()));
        log.info("Strava summary sync job started jobId={} userEmail={} accountLinkId={}", runningJob.getId(),
                runningJob.getUserEmail(), runningJob.getAccountLinkId());

        return runningJob;
    }

    private void sync(StravaSummarySyncJob job, StravaAccountLink accountLink) {
        Instant now = timeProvider.now();
        Instant after = now.atZone(ZoneOffset.UTC).minusMonths(INITIAL_SYNC_MONTHS).toInstant();

        try {
            List<StravaActivitySummaryImport> summaries = accessTokenService.executeWithValidToken(accountLink,
                    link -> activityProvider.fetchActivitySummaries(link.getAccessToken(), after, now));
            int importedCount = saveNewSummaries(accountLink, summaries, now);
            summarySyncJobRepository.save(job.completed(importedCount, timeProvider.now()));
            log.info("Strava summary sync job completed jobId={} userEmail={} accountLinkId={} importedActivityCount={}",
                    job.getId(), accountLink.getUserEmail(), accountLink.getId(), importedCount);
        } catch (StravaActivityRateLimitException exception) {
            failTransiently(job, accountLink, exception.partialSummaries(),
                    StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED);
        } catch (StravaActivityUnavailableException exception) {
            failTransiently(job, accountLink, exception.partialSummaries(),
                    StravaActivitySyncFailureReason.STRAVA_UNAVAILABLE);
        } catch (StravaReconnectRequiredException exception) {
            markPermanentFailure(job, StravaActivitySyncFailureReason.RECONNECT_REQUIRED);
        }
    }

    private void failTransiently(StravaSummarySyncJob job, StravaAccountLink accountLink,
                                 List<StravaActivitySummaryImport> partialSummaries,
                                 StravaActivitySyncFailureReason failureReason) {
        int importedCount = saveNewSummaries(accountLink, partialSummaries, timeProvider.now());

        if (job.getAttemptCount() >= maxAttempts) {
            summarySyncJobRepository.save(job.permanentFailure(failureReason.name(), importedCount,
                    timeProvider.now()));
            log.warn("Strava summary sync job failed permanently jobId={} userEmail={} accountLinkId={} failureReason={} importedActivityCount={}",
                    job.getId(), accountLink.getUserEmail(), accountLink.getId(), failureReason, importedCount);

            return;
        }

        Instant nextRunAt = timeProvider.now().plus(retryBackoff);
        summarySyncJobRepository.save(job.retryableFailure(failureReason.name(), importedCount, nextRunAt,
                timeProvider.now()));
        log.warn("Strava summary sync job scheduled for retry jobId={} userEmail={} accountLinkId={} failureReason={} nextRunAt={}",
                job.getId(), accountLink.getUserEmail(), accountLink.getId(), failureReason, nextRunAt);
    }

    private int saveNewSummaries(StravaAccountLink accountLink, List<StravaActivitySummaryImport> summaries,
                                 Instant importedAt) {
        if (summaries.isEmpty()) {
            return 0;
        }

        return transactionTemplate.execute(status -> summaries.stream()
                .map(summary -> toActivitySummary(accountLink, summary, importedAt))
                .mapToInt(this::saveNewSummary)
                .sum());
    }

    private int saveNewSummary(StravaActivitySummary activitySummary) {
        if (!activitySummaryRepository.saveIfAbsent(activitySummary)) {
            return 0;
        }

        if (streamEligibility.requiresStream(activitySummary.getActivityType())) {
            activitySummaryRepository.findByUserEmailAndSourceActivityId(activitySummary.getUserEmail(),
                            activitySummary.getSourceActivityId())
                    .map(savedSummary -> StravaActivityStreamSyncJob.normal(savedSummary, timeProvider.now()))
                    .ifPresent(streamSyncJobRepository::enqueueIfAbsent);
        }

        return 1;
    }

    private StravaActivitySummary toActivitySummary(StravaAccountLink accountLink, StravaActivitySummaryImport summary,
                                                    Instant importedAt) {
        return StravaActivitySummary.imported(accountLink.getUserEmail(), accountLink.getId(),
                summary.sourceActivityId(), summary.activityType(), summary.rawSportType(), summary.name(),
                summary.startDate(), summary.distanceMeters(), summary.movingTimeSeconds(),
                summary.averageSpeedMetersPerSecond(), summary.totalElevationGainMeters(),
                summary.maxSpeedMetersPerSecond(), summary.averageHeartRate(), summary.maxHeartRate(),
                summary.averageCadence(), summary.averageWatts(), summary.calories(), importedAt);
    }

    private void markPermanentFailure(StravaSummarySyncJob job, StravaActivitySyncFailureReason failureReason) {
        summarySyncJobRepository.save(job.permanentFailure(failureReason.name(), 0, timeProvider.now()));
        log.warn("Strava summary sync job failed jobId={} userEmail={} accountLinkId={} failureReason={}", job.getId(),
                job.getUserEmail(), job.getAccountLinkId(), failureReason);
    }

    private StravaActivitySyncFailureReason disabledFailureReason(Long accountLinkId) {
        return accountLinkRepository.findActiveById(accountLinkId)
                .filter(StravaAccountLink::isReconnectRequired)
                .map(link -> StravaActivitySyncFailureReason.RECONNECT_REQUIRED)
                .orElse(StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED);
    }
}
