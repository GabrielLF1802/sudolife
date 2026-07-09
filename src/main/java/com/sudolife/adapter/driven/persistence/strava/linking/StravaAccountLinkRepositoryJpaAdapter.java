package com.sudolife.adapter.driven.persistence.strava.linking;

import com.sudolife.adapter.driven.persistence.strava.linking.SpringDataStravaAccountLinkRepository;
import com.sudolife.adapter.driven.persistence.strava.linking.StravaAccountLinkPersistenceMapper;
import com.sudolife.adapter.driven.persistence.strava.linking.entitymodel.StravaAccountLinkEntity;
import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
import com.sudolife.application.service.strava.exception.InvalidStravaAccountLinkStateException;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StravaAccountLinkRepositoryJpaAdapter implements StravaAccountLinkRepository {

    private static final String ACTIVE_ATHLETE_CONSTRAINT = "uk_strava_account_links_active_athlete_id";
    private static final String ACTIVE_USER_CONSTRAINT = "uk_strava_account_links_active_user_email";

    private final SpringDataStravaAccountLinkRepository jpaRepository;
    private final StravaAccountLinkPersistenceMapper mapper;

    @Override
    public Optional<StravaAccountLink> findActiveById(Long id) {
        return jpaRepository.findByIdAndActiveTrue(id).map(mapper::toDomain);
    }

    @Override
    public Optional<StravaAccountLink> findActiveByUserEmail(String userEmail) {
        return jpaRepository.findByUserEmailAndActiveTrue(userEmail).map(mapper::toDomain);
    }

    @Override
    public Optional<StravaAccountLink> findActiveByAthleteId(Long athleteId) {
        return jpaRepository.findByAthleteIdAndActiveTrue(athleteId).map(mapper::toDomain);
    }

    @Override
    public List<StravaAccountLink> findAllActive() {
        return jpaRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public StravaAccountLink save(StravaAccountLink link) {
        try {
            StravaAccountLinkEntity entity = mapper.toEntity(link);
            StravaAccountLinkEntity savedEntity = jpaRepository.saveAndFlush(entity);

            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraintViolation(exception, ACTIVE_ATHLETE_CONSTRAINT)) {
                throw new DuplicateStravaAthleteOwnershipException();
            }

            if (hasConstraintViolation(exception, ACTIVE_USER_CONSTRAINT)) {
                throw new InvalidStravaAccountLinkStateException(exception);
            }

            throw exception;
        }
    }

    private boolean hasConstraintViolation(Throwable exception, String constraintName) {
        Throwable currentException = exception;
        while (currentException != null) {
            if (hasConstraintName(currentException, constraintName)) {
                return true;
            }

            currentException = currentException.getCause();
        }

        return false;
    }

    private boolean hasConstraintName(Throwable exception, String constraintName) {
        String message = exception.getMessage();

        return message != null && message.toLowerCase().contains(constraintName);
    }
}
