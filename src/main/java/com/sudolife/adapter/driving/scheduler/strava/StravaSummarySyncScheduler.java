package com.sudolife.adapter.driving.scheduler.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.EnqueueStravaSummarySyncCommand;
import com.sudolife.application.service.strava.ports.provided.EnqueueStravaSummarySyncUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "strava.summary-sync.scheduling-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StravaSummarySyncScheduler {

    private final StravaAccountLinkRepository accountLinkRepository;
    private final EnqueueStravaSummarySyncUseCase enqueueStravaSummarySyncUseCase;

    @Scheduled(fixedDelayString = "${strava.summary-sync.polling-interval:PT24H}")
    public void enqueueScheduledSummarySyncJobs() {
        int enqueuedCount = accountLinkRepository.findAllActive().stream()
                .filter(StravaAccountLink::canSyncActivities)
                .map(accountLink -> enqueueStravaSummarySyncUseCase.execute(
                        new EnqueueStravaSummarySyncCommand(accountLink.getId())))
                .mapToInt(result -> result.enqueued() ? 1 : 0)
                .sum();

        log.info("Strava scheduled summary sync enqueue completed enqueuedJobCount={}", enqueuedCount);
    }
}
