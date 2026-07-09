package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivityStreamImport;
import com.sudolife.application.model.strava.StravaActivityStreamSnapshot;
import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityStreamUnavailableException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.exception.StravaReconnectRequiredException;
import com.sudolife.application.service.strava.ports.provided.ProcessStravaActivityStreamSyncJobUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessStravaActivityStreamSyncJobUseCaseImpl implements ProcessStravaActivityStreamSyncJobUseCase {

    private final StravaActivityStreamSyncJobRepository streamSyncJobRepository;
    private final StravaActivityStreamSnapshotRepository streamSnapshotRepository;
    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaActivityProvider activityProvider;
    private final TimeProvider timeProvider;
    private final StravaAccessTokenService accessTokenService;
    @Value("${strava.stream-sync.max-attempts:3}")
    private int maxAttempts;
    @Value("${strava.stream-sync.retry-backoff:PT15M}")
    private Duration retryBackoff;

    @Override
    public void execute(ProcessStravaActivityStreamSyncJobCommand command) {
        Optional<StravaActivityStreamSyncJob> pendingJob = streamSyncJobRepository.findById(command.jobId());

        if (pendingJob.isEmpty() || !pendingJob.get().isQueuedOrRunning()) {
            return;
        }

        if (streamSnapshotRepository.findByActivitySummaryId(pendingJob.get().getActivitySummaryId()).isPresent()) {
            streamSyncJobRepository.save(pendingJob.get().completed(timeProvider.now()));

            return;
        }

        StravaActivityStreamSyncJob runningJob = streamSyncJobRepository.save(pendingJob.get().running(
                timeProvider.now()));
        accountLinkRepository.findActiveById(runningJob.getAccountLinkId())
                .filter(StravaAccountLink::canSyncActivities)
                .ifPresentOrElse(accountLink -> sync(runningJob, accountLink), () -> markPermanentFailure(runningJob,
                        disabledFailureReason(runningJob.getAccountLinkId())));
    }

    private void sync(StravaActivityStreamSyncJob job, StravaAccountLink accountLink) {
        try {
            StravaActivityStreamImport streamImport = accessTokenService.executeWithValidToken(accountLink,
                    link -> activityProvider.fetchActivityStreams(link.getAccessToken(), job.getSourceActivityId()));
            StravaActivityStreamSnapshot snapshot = StravaActivityStreamSnapshot.fetched(job.getActivitySummaryId(),
                    job.getAccountLinkId(), job.getUserEmail(), job.getSourceActivityId(), streamImport,
                    timeProvider.now());
            streamSnapshotRepository.saveIfAbsent(snapshot);
            streamSyncJobRepository.save(job.completed(timeProvider.now()));
            log.info("Strava stream sync job completed jobId={} userEmail={} activitySummaryId={}", job.getId(),
                    job.getUserEmail(), job.getActivitySummaryId());
        } catch (StravaActivityRateLimitException exception) {
            failTransiently(job, StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED);
        } catch (StravaActivityUnavailableException exception) {
            failTransiently(job, StravaActivitySyncFailureReason.STRAVA_UNAVAILABLE);
        } catch (StravaActivityStreamUnavailableException exception) {
            markPermanentFailure(job, StravaActivitySyncFailureReason.NO_STREAMS_AVAILABLE);
        } catch (StravaReconnectRequiredException exception) {
            markPermanentFailure(job, StravaActivitySyncFailureReason.RECONNECT_REQUIRED);
        }
    }

    private void failTransiently(StravaActivityStreamSyncJob job, StravaActivitySyncFailureReason failureReason) {
        if (job.getAttemptCount() >= maxAttempts) {
            streamSyncJobRepository.save(job.permanentFailure(failureReason.name(), timeProvider.now()));

            return;
        }

        Instant nextRunAt = timeProvider.now().plus(retryBackoff);
        streamSyncJobRepository.save(job.retryableFailure(failureReason.name(), nextRunAt, timeProvider.now()));
    }

    private void markPermanentFailure(StravaActivityStreamSyncJob job, StravaActivitySyncFailureReason failureReason) {
        streamSyncJobRepository.save(job.permanentFailure(failureReason.name(), timeProvider.now()));
    }

    private StravaActivitySyncFailureReason disabledFailureReason(Long accountLinkId) {
        return accountLinkRepository.findActiveById(accountLinkId)
                .filter(StravaAccountLink::isReconnectRequired)
                .map(link -> StravaActivitySyncFailureReason.RECONNECT_REQUIRED)
                .orElse(StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED);
    }
}
