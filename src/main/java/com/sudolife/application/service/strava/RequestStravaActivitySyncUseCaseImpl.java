package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.service.strava.ports.provided.RequestStravaActivitySyncUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestStravaActivitySyncUseCaseImpl implements RequestStravaActivitySyncUseCase {

    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaActivitySummaryRepository activitySummaryRepository;
    private final StravaSummarySyncJobRepository summarySyncJobRepository;
    private final TimeProvider timeProvider;

    @Override
    public StravaActivitySyncResult execute(RequestStravaActivitySyncCommand command) {
        return accountLinkRepository.findActiveByUserEmail(command.userEmail())
                .map(this::enqueueSync)
                .orElseGet(() -> result(StravaActivitySyncStatus.UNLINKED, null, 0, command.userEmail()));
    }

    private StravaActivitySyncResult enqueueSync(StravaAccountLink accountLink) {
        if (accountLink.isReconnectRequired()) {
            return result(StravaActivitySyncStatus.FAILED,
                    StravaActivitySyncFailureReason.RECONNECT_REQUIRED, 0, accountLink.getUserEmail());
        }

        if (!accountLink.hasActivityReadScope()) {
            return result(StravaActivitySyncStatus.FAILED,
                    StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED, 0, accountLink.getUserEmail());
        }

        boolean enqueued = summarySyncJobRepository.enqueueIfAbsent(StravaSummarySyncJob.queued(accountLink,
                timeProvider.now()));

        if (!enqueued) {
            log.info("Strava manual summary sync request coalesced userEmail={} accountLinkId={}",
                    accountLink.getUserEmail(), accountLink.getId());

            return result(StravaActivitySyncStatus.FAILED, StravaActivitySyncFailureReason.SYNC_ALREADY_RUNNING, 0,
                    accountLink.getUserEmail());
        }

        log.info("Strava manual summary sync job queued userEmail={} accountLinkId={}", accountLink.getUserEmail(),
                accountLink.getId());

        return result(StravaActivitySyncStatus.COMPLETED, null, 0, accountLink.getUserEmail());
    }

    private StravaActivitySyncResult result(StravaActivitySyncStatus status, StravaActivitySyncFailureReason failureReason,
                                            int importedCount, String userEmail) {
        return new StravaActivitySyncResult(status, failureReason, importedCount,
                activitySummaryRepository.countByUserEmail(userEmail));
    }
}
