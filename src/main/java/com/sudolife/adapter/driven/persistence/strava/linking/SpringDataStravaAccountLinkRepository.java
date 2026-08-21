package com.sudolife.adapter.driven.persistence.strava.linking;

import com.sudolife.adapter.driven.persistence.strava.linking.entitymodel.StravaAccountLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataStravaAccountLinkRepository extends JpaRepository<StravaAccountLinkEntity, Long> {

    Optional<StravaAccountLinkEntity> findByIdAndActiveTrue(Long id);

    Optional<StravaAccountLinkEntity> findByUserEmailAndActiveTrue(String userEmail);

    Optional<StravaAccountLinkEntity> findByAthleteIdAndActiveTrue(Long athleteId);

    List<StravaAccountLinkEntity> findByActiveTrue();

    List<StravaAccountLinkEntity> findByUserEmailOrderByLinkedAtAsc(String userEmail);

    @Query("""
            select link.id
            from StravaAccountLinkEntity link
            where link.userEmail = :userEmail
            """)
    List<Long> findIdsByUserEmail(String userEmail);

    void deleteByIdIn(List<Long> ids);
}
