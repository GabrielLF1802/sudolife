package com.sudolife.adapter.driven.api.strava.athlete;

import com.sudolife.adapter.driven.api.strava.StravaApiProperties;
import com.sudolife.adapter.driven.api.strava.athlete.dto.StravaAthleteZonesResponse;
import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.service.strava.exception.StravaAthleteProfileUnavailableException;
import com.sudolife.application.service.strava.ports.required.StravaAthleteProfileProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
public class StravaAthleteProfileAdapter implements StravaAthleteProfileProvider {

    private static final int REQUIRED_ZONE_COUNT = 5;

    private final StravaApiProperties properties;
    private final RestClient restClient;

    @Autowired
    public StravaAthleteProfileAdapter(StravaApiProperties properties) {
        this(properties, RestClient.builder()
                .requestFactory(requestFactory(properties))
                .build());
    }

    StravaAthleteProfileAdapter(StravaApiProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public List<TrainingHeartRateZone> fetchHeartRateZones(String accessToken) {
        try {
            StravaAthleteZonesResponse response = requestZones(accessToken);
            List<TrainingHeartRateZone> zones = toHeartRateZones(response);

            if (zones.size() != REQUIRED_ZONE_COUNT) {
                log.warn("Strava athlete zones response has no usable heart-rate zones usableZoneCount={}",
                        zones.size());
                throw new StravaAthleteProfileUnavailableException();
            }

            return zones;
        } catch (StravaAthleteProfileUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Strava athlete zones request failed category=client_error");
            throw new StravaAthleteProfileUnavailableException(exception);
        } catch (RuntimeException exception) {
            log.warn("Strava athlete zones response mapping failed");
            throw new StravaAthleteProfileUnavailableException(exception);
        }
    }

    private StravaAthleteZonesResponse requestZones(String accessToken) {
        StravaAthleteZonesResponse response = restClient.get()
                .uri(properties.athleteZonesUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(this::isForbidden, (request, httpResponse) -> {
                    log.warn("Strava athlete zones request forbidden statusCode={}", httpResponse.getStatusCode().value());
                    throw new StravaAthleteProfileUnavailableException();
                })
                .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                    log.warn("Strava athlete zones request failed statusCode={}", httpResponse.getStatusCode().value());
                    throw new StravaAthleteProfileUnavailableException();
                })
                .body(StravaAthleteZonesResponse.class);

        if (response == null) {
            throw new StravaAthleteProfileUnavailableException();
        }

        return response;
    }

    private List<TrainingHeartRateZone> toHeartRateZones(StravaAthleteZonesResponse response) {
        if (response.heartRate() == null || response.heartRate().zones() == null) {
            return List.of();
        }

        return response.heartRate().zones().stream()
                .filter(zone -> zone.min() != null && zone.max() != null)
                .filter(zone -> zone.max() > zone.min())
                .limit(REQUIRED_ZONE_COUNT)
                .map(zone -> new TrainingHeartRateZone(zone.min(), zone.max()))
                .toList();
    }

    private boolean isForbidden(HttpStatusCode statusCode) {
        return statusCode == HttpStatus.FORBIDDEN || statusCode == HttpStatus.UNAUTHORIZED;
    }

    private static SimpleClientHttpRequestFactory requestFactory(StravaApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return requestFactory;
    }
}
