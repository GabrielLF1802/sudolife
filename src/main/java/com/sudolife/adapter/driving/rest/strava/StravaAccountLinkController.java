package com.sudolife.adapter.driving.rest.strava;

import com.sudolife.adapter.driving.rest.strava.webmodel.StravaAuthorizationUrlResponse;
import com.sudolife.adapter.driving.rest.strava.webmodel.StravaCallbackRequest;
import com.sudolife.adapter.driving.rest.strava.webmodel.StravaLinkStatusResponse;
import com.sudolife.application.service.strava.CompleteStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.GetStravaAccountLinkStatusCommand;
import com.sudolife.application.service.strava.StartStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.StravaAuthorizationUrlResult;
import com.sudolife.application.service.strava.StravaCallbackResult;
import com.sudolife.application.service.strava.StravaLinkStatusResult;
import com.sudolife.application.service.strava.UnlinkStravaAccountCommand;
import com.sudolife.application.service.strava.ports.provided.CompleteStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.provided.GetStravaAccountLinkStatusUseCase;
import com.sudolife.application.service.strava.ports.provided.StartStravaAccountLinkingUseCase;
import com.sudolife.application.service.strava.ports.provided.UnlinkStravaAccountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

        return ResponseEntity.ok(new StravaLinkStatusResponse(result.linked(), result.athleteId()));
    }

    @DeleteMapping("/link")
    public ResponseEntity<Void> unlink(Authentication authentication) {
        unlinkStravaAccountUseCase.execute(new UnlinkStravaAccountCommand(authentication.getName()));

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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
}
