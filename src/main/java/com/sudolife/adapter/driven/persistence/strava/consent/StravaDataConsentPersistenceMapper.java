package com.sudolife.adapter.driven.persistence.strava.consent;

import com.sudolife.adapter.driven.persistence.strava.consent.entitymodel.StravaDataConsentRecordEntity;
import com.sudolife.application.model.strava.StravaDataConsentRecord;
import org.springframework.stereotype.Component;

@Component
public class StravaDataConsentPersistenceMapper {

    public StravaDataConsentRecordEntity toEntity(StravaDataConsentRecord domain) {
        StravaDataConsentRecordEntity entity = new StravaDataConsentRecordEntity();
        entity.setId(domain.getId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setPurpose(domain.getPurpose());
        entity.setConsentVersion(domain.getConsentVersion());
        entity.setLanguage(domain.getLanguage());
        entity.setConsentedAt(domain.getConsentedAt());
        entity.setSource(domain.getSource());

        return entity;
    }

    public StravaDataConsentRecord toDomain(StravaDataConsentRecordEntity entity) {
        return new StravaDataConsentRecord(entity.getId(), entity.getUserEmail(), entity.getPurpose(),
                entity.getConsentVersion(), entity.getLanguage(), entity.getConsentedAt(), entity.getSource());
    }
}
