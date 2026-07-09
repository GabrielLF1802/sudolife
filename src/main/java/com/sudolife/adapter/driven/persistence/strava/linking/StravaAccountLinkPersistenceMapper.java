package com.sudolife.adapter.driven.persistence.strava.linking;

import com.sudolife.adapter.driven.persistence.strava.linking.entitymodel.StravaAccountLinkEntity;
import com.sudolife.application.model.strava.StravaAccountLink;
import org.springframework.stereotype.Component;

@Component
public class StravaAccountLinkPersistenceMapper {

    public StravaAccountLinkEntity toEntity(StravaAccountLink domain) {
        StravaAccountLinkEntity entity = new StravaAccountLinkEntity();
        entity.setId(domain.getId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setAthleteId(domain.getAthleteId());
        entity.setActiveAthleteId(activeAthleteId(domain));
        entity.setActiveUserEmail(activeUserEmail(domain));
        entity.setAccessToken(domain.getAccessToken());
        entity.setRefreshToken(domain.getRefreshToken());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setGrantedScopes(domain.getGrantedScopes());
        entity.setActive(domain.isLinked());
        entity.setReconnectRequired(domain.isReconnectRequired());
        entity.setLinkedAt(domain.getLinkedAt());
        entity.setUnlinkedAt(domain.getUnlinkedAt());

        return entity;
    }

    public StravaAccountLink toDomain(StravaAccountLinkEntity entity) {
        return new StravaAccountLink(
                entity.getId(),
                entity.getUserEmail(),
                entity.getAthleteId(),
                entity.getAccessToken(),
                entity.getRefreshToken(),
                entity.getExpiresAt(),
                entity.getGrantedScopes(),
                entity.isActive(),
                entity.isReconnectRequired(),
                entity.getLinkedAt(),
                entity.getUnlinkedAt()
        );
    }

    private Long activeAthleteId(StravaAccountLink domain) {
        if (domain.isLinked()) {
            return domain.getAthleteId();
        }

        return null;
    }

    private String activeUserEmail(StravaAccountLink domain) {
        if (domain.isLinked()) {
            return domain.getUserEmail();
        }

        return null;
    }
}
