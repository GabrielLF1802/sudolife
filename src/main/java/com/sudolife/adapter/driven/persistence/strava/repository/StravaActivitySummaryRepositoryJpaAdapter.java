package com.sudolife.adapter.driven.persistence.strava.repository;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaActivitySummaryRepository;
import com.sudolife.adapter.driven.persistence.strava.StravaActivitySummaryPersistenceMapper;
import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivitySummaryEntity;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.StravaActivitySummaryPage;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public long countByAccountLinkId(Long accountLinkId) {
        return jpaRepository.countByAccountLinkId(accountLinkId);
    }

    @Override
    public long countStreamsReadyByAccountLinkId(Long accountLinkId) {
        return jpaRepository.countByAccountLinkIdAndActivityType(accountLinkId, StravaActivityType.WEIGHT_TRAINING);
    }

    @Override
    public StravaActivitySummaryPage findByUserEmail(String userEmail, int page, int size) {
        Page<StravaActivitySummaryEntity> result = jpaRepository.findByUserEmail(userEmail,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate").and(
                        Sort.by(Sort.Direction.DESC, "id"))));
        List<StravaActivitySummary> activities = result.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new StravaActivitySummaryPage(activities, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
