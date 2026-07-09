package com.sudolife.adapter.driving.rest.strava.linking;

import com.sudolife.adapter.driving.rest.strava.StravaFrontendRedirectProperties;
import com.sudolife.adapter.driving.rest.strava.webmodel.activity.StravaActivityDetailResponse;
import com.sudolife.adapter.driving.rest.strava.webmodel.activity.StravaActivityListItemResponse;
import com.sudolife.adapter.driving.rest.strava.webmodel.activity.StravaActivityListResponse;
import com.sudolife.adapter.driving.rest.strava.webmodel.activity.StravaActivitySyncResponse;
import com.sudolife.adapter.driving.rest.strava.webmodel.linking.StravaAuthorizationUrlResponse;
import com.sudolife.adapter.driving.rest.strava.webmodel.linking.StravaCallbackRequest;
import com.sudolife.adapter.driving.rest.strava.webmodel.linking.StravaLinkStatusResponse;
import com.sudolife.application.service.strava.activity.GetStravaActivityDetailCommand;
import com.sudolife.application.service.strava.activity.ListStravaActivitiesCommand;
import com.sudolife.application.service.strava.activity.RequestStravaActivitySyncCommand;
import com.sudolife.application.service.strava.activity.StravaActivityDetailResult;
import com.sudolife.application.service.strava.activity.StravaActivityListItemResult;
import com.sudolife.application.service.strava.activity.StravaActivityListResult;
import com.sudolife.application.service.strava.activity.StravaActivitySyncResult;
import com.sudolife.application.service.strava.linking.CompleteStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.linking.GetStravaAccountLinkStatusCommand;
import com.sudolife.application.service.strava.linking.StartStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.linking.StravaAuthorizationUrlResult;
import com.sudolife.application.service.strava.linking.StravaCallbackResult;
import com.sudolife.application.service.strava.linking.StravaLinkStatusResult;
import com.sudolife.application.service.strava.linking.UnlinkStravaAccountCommand;
import com.sudolife.application.service.strava.ports.provided.CompleteStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.provided.GetStravaAccountLinkStatusUseCase;
import com.sudolife.application.service.strava.ports.provided.GetStravaActivityDetailUseCase;
import com.sudolife.application.service.strava.ports.provided.ListStravaActivitiesUseCase;
import com.sudolife.application.service.strava.ports.provided.RequestStravaActivitySyncUseCase;
import com.sudolife.application.service.strava.ports.provided.StartStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.provided.UnlinkStravaAccountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/strava")
public class StravaAccountLinkController {

    private static final String SUCCESS_OUTCOME = "success";
    private static final String FAILURE_OUTCOME = "failure";

    private final StartStravaAccountLinkingUseCase startStravaAccountLinkingUseCase;
    private final CompleteStravaAccountLinkingUseCase completeStravaAccountLinkingUseCase;
    private final GetStravaAccountLinkStatusUseCase getStravaAccountLinkStatusUseCase;
    private final UnlinkStravaAccountUseCase unlinkStravaAccountUseCase;
    private final RequestStravaActivitySyncUseCase requestStravaActivitySyncUseCase;
    private final ListStravaActivitiesUseCase listStravaActivitiesUseCase;
    private final GetStravaActivityDetailUseCase getStravaActivityDetailUseCase;
    private final StravaFrontendRedirectProperties stravaFrontendRedirectProperties;

    @PostMapping("/link")
    public ResponseEntity<StravaAuthorizationUrlResponse> startLinking(Authentication authentication) {
        StravaAuthorizationUrlResult result = startStravaAccountLinkingUseCase.execute(
                new StartStravaAccountLinkingCommand(authentication.getName())
        );

        return ResponseEntity.ok(new StravaAuthorizationUrlResponse(result.authorizationUrl()));
    }

    @GetMapping("/status")
    public ResponseEntity<StravaLinkStatusResponse> status(Authentication authentication) {
        StravaLinkStatusResult result = getStravaAccountLinkStatusUseCase.execute(
                new GetStravaAccountLinkStatusCommand(authentication.getName())
        );

        return ResponseEntity.ok(new StravaLinkStatusResponse(result.linked(), result.athleteId(),
                result.permissionState().name(), result.profilePermissionState().name(),
                result.activitySummaryStatus().name(), result.performanceDataStatus().name(), result.lastSummarySyncTime(),
                result.lastStreamEnrichmentTime(), result.importedActivityCount(),
                result.streamsReadyActivityCount(), failureReason(result)));
    }

    @DeleteMapping("/link")
    public ResponseEntity<Void> unlink(Authentication authentication) {
        unlinkStravaAccountUseCase.execute(new UnlinkStravaAccountCommand(authentication.getName()));

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/sync")
    public ResponseEntity<StravaActivitySyncResponse> sync(Authentication authentication) {
        StravaActivitySyncResult result = requestStravaActivitySyncUseCase.execute(
                new RequestStravaActivitySyncCommand(authentication.getName())
        );

        return ResponseEntity.ok(new StravaActivitySyncResponse(result.status().name(), failureReason(result),
                result.importedActivityCount(), result.totalActivityCount()));
    }

    @GetMapping("/activities")
    public ResponseEntity<StravaActivityListResponse> activities(Authentication authentication, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        StravaActivityListResult result = listStravaActivitiesUseCase.execute(
                new ListStravaActivitiesCommand(authentication.getName(), page, size)
        );

        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<StravaActivityDetailResponse> activity(Authentication authentication, @PathVariable Long activityId) {
        StravaActivityDetailResult result = getStravaActivityDetailUseCase.execute(
                new GetStravaActivityDetailCommand(authentication.getName(), activityId)
        );

        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping("/callback")
    public RedirectView callback(@ModelAttribute StravaCallbackRequest request) {
        StravaCallbackResult result = completeStravaAccountLinkingUseCase.execute(
                new CompleteStravaAccountLinkingCommand(request.state(), request.code(), request.scope(),
                        request.error())
        );

        return new RedirectView(redirectUrl(result));
    }

    private String redirectUrl(StravaCallbackResult result) {
        if (result.linked()) {
            return UriComponentsBuilder.fromUriString(stravaFrontendRedirectProperties.frontendSuccessRedirectUrl())
                    .queryParam("outcome", SUCCESS_OUTCOME)
                    .build()
                    .toUriString();
        }

        return UriComponentsBuilder.fromUriString(stravaFrontendRedirectProperties.frontendFailureRedirectUrl())
                .queryParam("outcome", FAILURE_OUTCOME)
                .queryParam("failureCode", result.failureCode())
                .build()
                .toUriString();
    }

    private String failureReason(StravaActivitySyncResult result) {
        if (result.failureReason() == null) {
            return null;
        }

        return result.failureReason().name();
    }

    private String failureReason(StravaLinkStatusResult result) {
        if (result.failureReason() == null) {
            return null;
        }

        return result.failureReason().name();
    }

    private StravaActivityListResponse toResponse(StravaActivityListResult result) {
        return new StravaActivityListResponse(result.activities().stream()
                .map(this::toResponse)
                .toList(), result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    private StravaActivityListItemResponse toResponse(StravaActivityListItemResult result) {
        return new StravaActivityListItemResponse(result.id(), result.sourceActivityId(), result.name(),
                result.sportType().name(), result.startDate(), result.distanceMeters(), result.movingTimeSeconds(),
                result.averageSpeedMetersPerSecond(), result.averagePaceSecondsPerKilometer(),
                result.streamStatus().name());
    }

    private StravaActivityDetailResponse toResponse(StravaActivityDetailResult result) {
        return new StravaActivityDetailResponse(result.id(), result.sourceActivityId(), result.name(),
                result.sportType().name(), result.startDate(), result.distanceMeters(), result.movingTimeSeconds(),
                result.totalElevationGainMeters(), result.averageSpeedMetersPerSecond(),
                result.averagePaceSecondsPerKilometer(), result.maxSpeedMetersPerSecond(),
                result.averageHeartRate(), result.maxHeartRate(), result.averageCadence(), result.averageWatts(),
                result.calories(), result.streamStatus().name(), result.availableStreamMetricNames(),
                result.enrichmentStatus().name());
    }
}
