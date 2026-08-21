package com.sudolife.application.service.user.ports.required;

import com.sudolife.application.service.user.StravaDeauthorization;

import java.time.Instant;
import java.util.List;

public interface AccountDeletionDataRepository {

    List<StravaDeauthorization> findStravaDeauthorizations(String userEmail);

    void deleteAccountOwnedData(String userEmail, Instant now);
}
