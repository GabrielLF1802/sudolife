package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.service.strava.ports.provided.EnqueueStravaSummarySyncUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnqueueStravaSummarySyncUseCaseImpl implements EnqueueStravaSummarySyncUseCase {

    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaSummarySyncJobRepository summarySyncJobRepository;
    private final TimeProvider timeProvider;

    @Override
    public EnqueueStravaSummarySyncResult execute(EnqueueStravaSummarySyncCommand command) {
        return accountLinkRepository.findActiveById(command.accountLinkId())
                .filter(StravaAccountLink::hasActivityReadScope)
                .map(this::enqueue)
                .orElseGet(() -> new EnqueueStravaSummarySyncResult(false));
    }

    private EnqueueStravaSummarySyncResult enqueue(StravaAccountLink accountLink) {
        boolean enqueued = summarySyncJobRepository.enqueueIfAbsent(StravaSummarySyncJob.queued(accountLink,
                timeProvider.now()));

        if (enqueued) {
            log.info("Strava summary sync job queued userEmail={} accountLinkId={}", accountLink.getUserEmail(),
                    accountLink.getId());
        }

        return new EnqueueStravaSummarySyncResult(enqueued);
    }
}
