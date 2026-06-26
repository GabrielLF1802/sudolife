package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.ports.provided.RequestStravaActivitySyncUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestStravaActivitySyncUseCaseImpl implements RequestStravaActivitySyncUseCase {

    private static final long INITIAL_SYNC_MONTHS = 7L;

    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaActivityProvider activityProvider;
    private final StravaActivitySummaryRepository activitySummaryRepository;
    private final TimeProvider timeProvider;
    private final TransactionTemplate transactionTemplate;

    @Override
    public StravaActivitySyncResult execute(RequestStravaActivitySyncCommand command) {
        return accountLinkRepository.findActiveByUserEmail(command.userEmail())
                .map(this::sync)
                .orElseGet(() -> result(StravaActivitySyncStatus.UNLINKED, null, 0, command.userEmail()));
    }

    private StravaActivitySyncResult sync(StravaAccountLink accountLink) {
        if (!accountLink.hasActivityReadScope()) {
            return result(StravaActivitySyncStatus.FAILED,
                    StravaActivitySyncFailureReason.PERMISSION_UPGRADE_REQUIRED, 0, accountLink.getUserEmail());
        }

        Instant now = timeProvider.now();
        Instant after = now.atZone(ZoneOffset.UTC).minusMonths(INITIAL_SYNC_MONTHS).toInstant();

        try {
            List<StravaActivitySummaryImport> summaries = activityProvider.fetchActivitySummaries(
                    accountLink.getAccessToken(), after, now);
            int importedCount = saveNewSummaries(accountLink, summaries, now);
            log.info("Strava manual summary sync completed for userEmail={} athleteId={} importedActivityCount={}",
                    accountLink.getUserEmail(), accountLink.getAthleteId(), importedCount);

            return result(StravaActivitySyncStatus.COMPLETED, null, importedCount, accountLink.getUserEmail());
        } catch (StravaActivityRateLimitException exception) {
            int importedCount = saveNewSummaries(accountLink, exception.partialSummaries(), now);
            log.warn("Strava manual summary sync rate limited for userEmail={} athleteId={}",
                    accountLink.getUserEmail(), accountLink.getAthleteId());

            return result(StravaActivitySyncStatus.FAILED, StravaActivitySyncFailureReason.STRAVA_RATE_LIMITED,
                    importedCount, accountLink.getUserEmail());
        } catch (StravaActivityUnavailableException exception) {
            int importedCount = saveNewSummaries(accountLink, exception.partialSummaries(), now);
            log.warn("Strava manual summary sync unavailable for userEmail={} athleteId={}",
                    accountLink.getUserEmail(), accountLink.getAthleteId());

            return result(StravaActivitySyncStatus.FAILED, StravaActivitySyncFailureReason.STRAVA_UNAVAILABLE,
                    importedCount, accountLink.getUserEmail());
        }
    }

    private int saveNewSummaries(StravaAccountLink accountLink, List<StravaActivitySummaryImport> summaries, Instant importedAt) {
        if (summaries.isEmpty()) {
            return 0;
        }

        return transactionTemplate.execute(status -> summaries.stream()
                .map(summary -> toActivitySummary(accountLink, summary, importedAt))
                .mapToInt(activitySummary -> activitySummaryRepository.saveIfAbsent(activitySummary) ? 1 : 0)
                .sum());
    }

    private StravaActivitySummary toActivitySummary(StravaAccountLink accountLink, StravaActivitySummaryImport summary, Instant importedAt) {
        return StravaActivitySummary.imported(accountLink.getUserEmail(), accountLink.getId(),
                summary.sourceActivityId(), summary.activityType(), summary.rawSportType(), summary.name(),
                summary.startDate(), summary.distanceMeters(), summary.movingTimeSeconds(),
                summary.averageSpeedMetersPerSecond(), summary.totalElevationGainMeters(),
                summary.maxSpeedMetersPerSecond(), summary.averageHeartRate(), summary.maxHeartRate(),
                summary.averageCadence(), summary.averageWatts(), summary.calories(), importedAt);
    }

    private StravaActivitySyncResult result(StravaActivitySyncStatus status, StravaActivitySyncFailureReason failureReason, int importedCount, String userEmail) {
        return new StravaActivitySyncResult(status, failureReason, importedCount,
                activitySummaryRepository.countByUserEmail(userEmail));
    }
}
