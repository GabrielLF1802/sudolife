package com.sudolife.adapter.driving.scheduler.strava;

import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.service.strava.ProcessStravaSummarySyncJobCommand;
import com.sudolife.application.service.strava.ports.provided.ProcessStravaSummarySyncJobUseCase;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "strava.summary-sync.scheduling-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StravaSummarySyncWorker {

    private final StravaSummarySyncJobRepository summarySyncJobRepository;
    private final ProcessStravaSummarySyncJobUseCase processStravaSummarySyncJobUseCase;
    private final TimeProvider timeProvider;

    @Scheduled(fixedDelayString = "${strava.summary-sync.worker-interval:PT1M}")
    public void processNextSummarySyncJob() {
        summarySyncJobRepository.findNextRunnable(timeProvider.now())
                .ifPresent(this::process);
    }

    private void process(StravaSummarySyncJob job) {
        log.info("Strava summary sync worker picked job jobId={} userEmail={} accountLinkId={}", job.getId(),
                job.getUserEmail(), job.getAccountLinkId());
        processStravaSummarySyncJobUseCase.execute(new ProcessStravaSummarySyncJobCommand(job.getId()));
    }
}
