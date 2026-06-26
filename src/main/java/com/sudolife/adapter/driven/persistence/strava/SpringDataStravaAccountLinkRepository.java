package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAccountLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataStravaAccountLinkRepository extends JpaRepository<StravaAccountLinkEntity, Long> {

    Optional<StravaAccountLinkEntity> findByIdAndActiveTrue(Long id);

    Optional<StravaAccountLinkEntity> findByUserEmailAndActiveTrue(String userEmail);

    Optional<StravaAccountLinkEntity> findByAthleteIdAndActiveTrue(Long athleteId);

    List<StravaAccountLinkEntity> findByActiveTrue();

    List<StravaAccountLinkEntity> findByUserEmailOrderByLinkedAtAsc(String userEmail);
}
