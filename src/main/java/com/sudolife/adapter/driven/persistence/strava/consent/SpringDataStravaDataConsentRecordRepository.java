package com.sudolife.adapter.driven.persistence.strava.consent;

import com.sudolife.adapter.driven.persistence.strava.consent.entitymodel.StravaDataConsentRecordEntity;
import com.sudolife.application.model.strava.StravaDataConsentPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStravaDataConsentRecordRepository extends JpaRepository<StravaDataConsentRecordEntity, Long> {

    boolean existsByUserEmailAndPurposeAndConsentVersion(String userEmail, StravaDataConsentPurpose purpose, String consentVersion);

    void deleteByUserEmail(String userEmail);
}
