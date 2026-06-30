package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivityDetailImport;
import com.sudolife.application.model.strava.StravaActivityDetailSnapshot;
import com.sudolife.application.model.strava.StravaActivityStreamImport;
import com.sudolife.application.model.strava.StravaActivityStreamSnapshot;
import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.service.strava.exception.StravaActivityNotFoundException;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.ports.provided.GetStravaActivityDetailUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityDetailSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSyncJobRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetStravaActivityDetailUseCaseImpl implements GetStravaActivityDetailUseCase {

    private final StravaActivitySummaryRepository activitySummaryRepository;
    private final StravaActivityDetailSnapshotRepository detailSnapshotRepository;
    private final StravaActivityStreamSnapshotRepository streamSnapshotRepository;
    private final StravaActivityStreamSyncJobRepository streamSyncJobRepository;
    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaActivityProvider activityProvider;
    private final TimeProvider timeProvider;
    private final StravaActivityStreamEligibility streamEligibility = new StravaActivityStreamEligibility();

    @Override
    public StravaActivityDetailResult execute(GetStravaActivityDetailCommand command) {
        StravaActivitySummary summary = activitySummaryRepository.findByIdAndUserEmail(command.activityId(),
                command.userEmail()).orElseThrow(StravaActivityNotFoundException::new);
        Optional<StravaActivityDetailSnapshot> existingSnapshot = detailSnapshotRepository.findByActivitySummaryId(
                summary.getId());

        if (existingSnapshot.isPresent()) {
            prioritizeMissingStream(summary);

            return toDetailResult(summary, existingSnapshot.get(), StravaActivityDetailEnrichmentStatus.COMPLETED);
        }

        return fetchAndPersistDetail(summary).orElseGet(() -> summaryFallback(summary));
    }

    private Optional<StravaActivityDetailResult> fetchAndPersistDetail(StravaActivitySummary summary) {
        Optional<StravaAccountLink> accountLink = accountLinkRepository.findActiveByUserEmail(summary.getUserEmail());
        if (accountLink.isEmpty()) {
            return Optional.empty();
        }

        try {
            StravaActivityDetailImport detail = activityProvider.fetchActivityDetail(accountLink.get().getAccessToken(),
                    summary.getSourceActivityId());
            StravaActivityDetailSnapshot snapshot = StravaActivityDetailSnapshot.fetched(summary.getId(),
                    summary.getUserEmail(), detail, timeProvider.now());
            StravaActivityDetailSnapshot savedSnapshot = detailSnapshotRepository.saveIfAbsent(snapshot);

            prioritizeMissingStream(summary, accountLink.get());

            return Optional.of(toDetailResult(summary, savedSnapshot,
                    StravaActivityDetailEnrichmentStatus.COMPLETED));
        } catch (StravaActivityRateLimitException | StravaActivityUnavailableException exception) {
            return Optional.empty();
        }
    }

    private StravaActivityDetailResult summaryFallback(StravaActivitySummary summary) {
        return new StravaActivityDetailResult(summary.getId(), summary.getSourceActivityId(), summary.getName(),
                summary.getActivityType(), summary.getStartDate(), summary.getDistanceMeters(),
                summary.getMovingTimeSeconds(), summary.getTotalElevationGainMeters(),
                summary.getAverageSpeedMetersPerSecond(), summary.getPaceSecondsPerKilometer(),
                summary.getMaxSpeedMetersPerSecond(), summary.getAverageHeartRate(), summary.getMaxHeartRate(),
                summary.getAverageCadence(), summary.getAverageWatts(), summary.getCalories(), streamStatus(summary),
                availableMetricNames(summary.getId()), StravaActivityDetailEnrichmentStatus.SUMMARY_FALLBACK);
    }

    private StravaActivityDetailResult toDetailResult(StravaActivitySummary summary, StravaActivityDetailSnapshot snapshot,
                                                      StravaActivityDetailEnrichmentStatus enrichmentStatus) {
        return new StravaActivityDetailResult(summary.getId(), snapshot.getSourceActivityId(), snapshot.getName(),
                snapshot.getActivityType(), snapshot.getStartDate(), snapshot.getDistanceMeters(),
                snapshot.getMovingTimeSeconds(), snapshot.getTotalElevationGainMeters(),
                snapshot.getAverageSpeedMetersPerSecond(), snapshot.getPaceSecondsPerKilometer(),
                snapshot.getMaxSpeedMetersPerSecond(), snapshot.getAverageHeartRate(), snapshot.getMaxHeartRate(),
                snapshot.getAverageCadence(), snapshot.getAverageWatts(), snapshot.getCalories(),
                streamStatus(summary), availableMetricNames(summary.getId()), enrichmentStatus);
    }

    private StravaActivityStreamStatus streamStatus(StravaActivitySummary activity) {
        if (!streamEligibility.requiresStream(activity.getActivityType())) {
            return StravaActivityStreamStatus.NOT_REQUIRED;
        }

        return streamSnapshotRepository.findByActivitySummaryId(activity.getId())
                .map(snapshot -> StravaActivityStreamStatus.COMPLETED)
                .orElse(StravaActivityStreamStatus.PENDING);
    }

    private List<String> availableMetricNames(Long activitySummaryId) {
        return streamSnapshotRepository.findByActivitySummaryId(activitySummaryId)
                .map(StravaActivityStreamSnapshot::getAvailableMetricNames)
                .orElse(List.of());
    }

    private void prioritizeMissingStream(StravaActivitySummary summary) {
        accountLinkRepository.findActiveByUserEmail(summary.getUserEmail())
                .ifPresent(accountLink -> prioritizeMissingStream(summary, accountLink));
    }

    private void prioritizeMissingStream(StravaActivitySummary summary, StravaAccountLink accountLink) {
        if (!streamEligibility.requiresStream(summary.getActivityType()) ||
                streamSnapshotRepository.findByActivitySummaryId(summary.getId()).isPresent()) {
            return;
        }

        try {
            StravaActivityStreamImport streamImport = activityProvider.fetchActivityStreams(accountLink.getAccessToken(),
                    summary.getSourceActivityId());
            StravaActivityStreamSnapshot snapshot = StravaActivityStreamSnapshot.fetched(summary.getId(),
                    summary.getAccountLinkId(), summary.getUserEmail(), summary.getSourceActivityId(), streamImport,
                    timeProvider.now());
            streamSnapshotRepository.saveIfAbsent(snapshot);
        } catch (StravaActivityRateLimitException | StravaActivityUnavailableException exception) {
            streamSyncJobRepository.enqueueIfAbsent(StravaActivityStreamSyncJob.highPriority(summary,
                    timeProvider.now()));
        }
    }
}
