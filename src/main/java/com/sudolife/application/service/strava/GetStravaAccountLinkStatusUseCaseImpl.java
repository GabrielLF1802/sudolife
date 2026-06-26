package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.ports.provided.GetStravaAccountLinkStatusUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStravaAccountLinkStatusUseCaseImpl implements GetStravaAccountLinkStatusUseCase {

    private final StravaAccountLinkRepository accountLinkRepository;

    @Override
    public StravaLinkStatusResult execute(GetStravaAccountLinkStatusCommand command) {
        return accountLinkRepository.findActiveByUserEmail(command.userEmail())
                .map(this::linkedStatus)
                .orElseGet(this::unlinkedStatus);
    }

    private StravaLinkStatusResult linkedStatus(StravaAccountLink accountLink) {
        return new StravaLinkStatusResult(true, accountLink.getAthleteId(), permissionState(accountLink));
    }

    private StravaLinkStatusResult unlinkedStatus() {
        return new StravaLinkStatusResult(false, null, StravaPermissionState.UNLINKED);
    }

    private StravaPermissionState permissionState(StravaAccountLink accountLink) {
        if (accountLink.hasActivityReadScope()) {
            return StravaPermissionState.READY;
        }

        return StravaPermissionState.PERMISSION_UPGRADE_REQUIRED;
    }
}
