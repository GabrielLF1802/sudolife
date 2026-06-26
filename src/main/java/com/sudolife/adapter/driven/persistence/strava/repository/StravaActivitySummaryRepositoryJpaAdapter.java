package com.sudolife.adapter.driven.persistence.strava.repository;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaActivitySummaryRepository;
import com.sudolife.adapter.driven.persistence.strava.StravaActivitySummaryPersistenceMapper;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StravaActivitySummaryRepositoryJpaAdapter implements StravaActivitySummaryRepository {

    private final SpringDataStravaActivitySummaryRepository jpaRepository;
    private final StravaActivitySummaryPersistenceMapper mapper;

    @Override
    public boolean saveIfAbsent(StravaActivitySummary activitySummary) {
        if (jpaRepository.existsByUserEmailAndSourceActivityId(activitySummary.getUserEmail(),
                activitySummary.getSourceActivityId())) {
            return false;
        }

        try {
            jpaRepository.saveAndFlush(mapper.toEntity(activitySummary));

            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    @Override
    public long countByUserEmail(String userEmail) {
        return jpaRepository.countByUserEmail(userEmail);
    }
}
