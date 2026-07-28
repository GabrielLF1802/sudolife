package com.sudolife.adapter.driving.scheduler.training;

import com.sudolife.application.service.training.ports.provided.HandleMissedPlannedSessionsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "adaptive-coaching.missed-session-scheduling-enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MissedPlannedSessionWorker {

    private final HandleMissedPlannedSessionsUseCase handleMissedPlannedSessionsUseCase;

    @Scheduled(fixedDelayString = "${adaptive-coaching.missed-session-worker-interval:PT1H}")
    public void handleMissedPlannedSessions() {
        log.info("Handling missed planned sessions");
        handleMissedPlannedSessionsUseCase.execute();
    }
}
