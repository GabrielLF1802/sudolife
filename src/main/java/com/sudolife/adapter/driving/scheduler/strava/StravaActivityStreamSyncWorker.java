package com.sudolife.adapter.driving.scheduler.strava;

import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;
import com.sudolife.application.service.strava.ProcessStravaActivityStreamSyncJobCommand;
import com.sudolife.application.service.strava.ports.provided.ProcessStravaActivityStreamSyncJobUseCase;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "strava.stream-sync.scheduling-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StravaActivityStreamSyncWorker {

    private final StravaActivityStreamSyncJobRepository streamSyncJobRepository;
    private final ProcessStravaActivityStreamSyncJobUseCase processStravaActivityStreamSyncJobUseCase;
    private final TimeProvider timeProvider;

    @Scheduled(fixedDelayString = "${strava.stream-sync.worker-interval:PT1M}")
    public void processNextActivityStreamSyncJob() {
        streamSyncJobRepository.findNextRunnable(timeProvider.now())
                .ifPresent(this::process);
    }

    private void process(StravaActivityStreamSyncJob job) {
        log.info("Strava stream sync worker picked job jobId={} userEmail={} activitySummaryId={}", job.getId(),
                job.getUserEmail(), job.getActivitySummaryId());
        processStravaActivityStreamSyncJobUseCase.execute(new ProcessStravaActivityStreamSyncJobCommand(job.getId()));
    }
}
