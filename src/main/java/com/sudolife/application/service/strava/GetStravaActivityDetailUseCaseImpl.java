package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivityDetailImport;
import com.sudolife.application.model.strava.StravaActivityDetailSnapshot;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.exception.StravaActivityNotFoundException;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.ports.provided.GetStravaActivityDetailUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityDetailSnapshotRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
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
    private final StravaAccountLinkRepository accountLinkRepository;
    private final StravaActivityProvider activityProvider;
    private final TimeProvider timeProvider;

    @Override
    public StravaActivityDetailResult execute(GetStravaActivityDetailCommand command) {
        StravaActivitySummary summary = activitySummaryRepository.findByIdAndUserEmail(command.activityId(),
                command.userEmail()).orElseThrow(StravaActivityNotFoundException::new);
        Optional<StravaActivityDetailSnapshot> existingSnapshot = detailSnapshotRepository.findByActivitySummaryId(
                summary.getId());

        if (existingSnapshot.isPresent()) {
            return toDetailResult(summary.getId(), existingSnapshot.get(), StravaActivityDetailEnrichmentStatus.COMPLETED);
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

            return Optional.of(toDetailResult(summary.getId(), savedSnapshot,
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
                summary.getAverageCadence(), summary.getAverageWatts(), summary.getCalories(),
                streamStatus(summary), List.of(), StravaActivityDetailEnrichmentStatus.SUMMARY_FALLBACK);
    }

    private StravaActivityDetailResult toDetailResult(Long activityId, StravaActivityDetailSnapshot snapshot,
                                                      StravaActivityDetailEnrichmentStatus enrichmentStatus) {
        return new StravaActivityDetailResult(activityId, snapshot.getSourceActivityId(), snapshot.getName(),
                snapshot.getActivityType(), snapshot.getStartDate(), snapshot.getDistanceMeters(),
                snapshot.getMovingTimeSeconds(), snapshot.getTotalElevationGainMeters(),
                snapshot.getAverageSpeedMetersPerSecond(), snapshot.getPaceSecondsPerKilometer(),
                snapshot.getMaxSpeedMetersPerSecond(), snapshot.getAverageHeartRate(), snapshot.getMaxHeartRate(),
                snapshot.getAverageCadence(), snapshot.getAverageWatts(), snapshot.getCalories(),
                streamStatus(snapshot), List.of(), enrichmentStatus);
    }

    private StravaActivityStreamStatus streamStatus(StravaActivitySummary activity) {
        if (activity.getActivityType() == StravaActivityType.WEIGHT_TRAINING) {
            return StravaActivityStreamStatus.NOT_REQUIRED;
        }

        return StravaActivityStreamStatus.PENDING;
    }

    private StravaActivityStreamStatus streamStatus(StravaActivityDetailSnapshot activity) {
        if (activity.getActivityType() == StravaActivityType.WEIGHT_TRAINING) {
            return StravaActivityStreamStatus.NOT_REQUIRED;
        }

        return StravaActivityStreamStatus.PENDING;
    }
}
