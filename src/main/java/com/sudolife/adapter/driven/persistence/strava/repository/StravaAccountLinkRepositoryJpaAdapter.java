package com.sudolife.adapter.driven.persistence.strava.repository;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaAccountLinkRepository;
import com.sudolife.adapter.driven.persistence.strava.StravaAccountLinkPersistenceMapper;
import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAccountLinkEntity;
import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StravaAccountLinkRepositoryJpaAdapter implements StravaAccountLinkRepository {

    private static final String ACTIVE_ATHLETE_CONSTRAINT = "uk_strava_account_links_active_athlete_id";

    private final SpringDataStravaAccountLinkRepository jpaRepository;
    private final StravaAccountLinkPersistenceMapper mapper;

    @Override
    public Optional<StravaAccountLink> findActiveByUserEmail(String userEmail) {
        return jpaRepository.findByUserEmailAndActiveTrue(userEmail).map(mapper::toDomain);
    }

    @Override
    public Optional<StravaAccountLink> findActiveByAthleteId(Long athleteId) {
        return jpaRepository.findByAthleteIdAndActiveTrue(athleteId).map(mapper::toDomain);
    }

    @Override
    public StravaAccountLink save(StravaAccountLink link) {
        try {
            StravaAccountLinkEntity entity = mapper.toEntity(link);
            StravaAccountLinkEntity savedEntity = jpaRepository.saveAndFlush(entity);

            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            if (!hasActiveAthleteConstraintViolation(exception)) {
                throw exception;
            }

            throw new DuplicateStravaAthleteOwnershipException();
        }
    }

    private boolean hasActiveAthleteConstraintViolation(Throwable exception) {
        Throwable currentException = exception;
        while (currentException != null) {
            if (hasActiveAthleteConstraintName(currentException)) {
                return true;
            }

            currentException = currentException.getCause();
        }

        return false;
    }

    private boolean hasActiveAthleteConstraintName(Throwable exception) {
        String message = exception.getMessage();

        return message != null && message.toLowerCase().contains(ACTIVE_ATHLETE_CONSTRAINT);
    }
}
