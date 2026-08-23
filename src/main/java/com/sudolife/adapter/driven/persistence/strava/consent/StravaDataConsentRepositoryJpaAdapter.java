package com.sudolife.adapter.driven.persistence.strava.consent;

import com.sudolife.application.model.strava.StravaDataConsentPurpose;
import com.sudolife.application.model.strava.StravaDataConsentRecord;
import com.sudolife.application.service.strava.ports.required.StravaDataConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StravaDataConsentRepositoryJpaAdapter implements StravaDataConsentRepository {

    private final SpringDataStravaDataConsentRecordRepository jpaRepository;
    private final StravaDataConsentPersistenceMapper mapper;

    @Override
    public boolean existsByUserEmailAndPurposeAndConsentVersion(String userEmail, StravaDataConsentPurpose purpose,
                                                               String consentVersion) {
        return jpaRepository.existsByUserEmailAndPurposeAndConsentVersion(userEmail, purpose, consentVersion);
    }

    @Override
    public StravaDataConsentRecord save(StravaDataConsentRecord consentRecord) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(consentRecord)));
    }
}
