package com.sudolife.application.service.strava;

public record StravaLinkStatusResult(boolean linked, Long athleteId, StravaPermissionState permissionState) {
}
