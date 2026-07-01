package com.sudolife.application.service.strava.ports.required;

public interface StravaImportedDataRepository {

    void deleteByAccountLinkId(Long accountLinkId);
}
