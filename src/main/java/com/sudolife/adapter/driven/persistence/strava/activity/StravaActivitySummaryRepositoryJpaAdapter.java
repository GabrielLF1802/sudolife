package com.sudolife.adapter.driven.persistence.strava.activity;

import com.sudolife.adapter.driven.persistence.strava.activity.entitymodel.StravaActivitySummaryEntity;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.activity.StravaActivitySummaryPage;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

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

    @Override
    public Optional<StravaActivitySummary> findByIdAndUserEmail(Long id, String userEmail) {
        return jpaRepository.findByIdAndUserEmail(id, userEmail)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<StravaActivitySummary> findByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId) {
        return jpaRepository.findByUserEmailAndSourceActivityId(userEmail, sourceActivityId)
                .map(mapper::toDomain);
    }

    @Override
    public List<StravaActivitySummary> findByUserEmailAndActivityTypeAndStartDateBetween(
            String userEmail, StravaActivityType activityType, Instant startDate, Instant endDate
    ) {
        return jpaRepository.findByUserEmailAndActivityTypeAndStartDateBetweenOrderByStartDateDesc(
                        userEmail, activityType, startDate, endDate)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
