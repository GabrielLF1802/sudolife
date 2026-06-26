package com.sudolife.adapter.driven.api.strava;

import com.sudolife.adapter.driven.api.strava.dto.StravaActivitySummaryResponse;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.StravaActivitySummaryImport;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class StravaActivityAdapter implements StravaActivityProvider {

    private static final int PAGE_SIZE = 200;

    private final StravaApiProperties properties;
    private final RestClient restClient;

    @Autowired
    public StravaActivityAdapter(StravaApiProperties properties) {
        this(properties, RestClient.builder()
                .requestFactory(requestFactory(properties))
                .build());
    }

    StravaActivityAdapter(StravaApiProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public List<StravaActivitySummaryImport> fetchActivitySummaries(String accessToken, Instant after, Instant before) {
        List<StravaActivitySummaryImport> summaries = new ArrayList<>();
        int page = 1;
        StravaActivitySummaryResponse[] responses;

        do {
            try {
                responses = requestPage(accessToken, after, before, page);
            } catch (StravaActivityRateLimitException exception) {
                throw new StravaActivityRateLimitException(summaries);
            } catch (StravaActivityUnavailableException exception) {
                throw new StravaActivityUnavailableException(summaries, exception);
            }
            Arrays.stream(responses)
                    .map(this::toImport)
                    .flatMap(Optional::stream)
                    .forEach(summaries::add);
            page++;
        } while (responses.length == PAGE_SIZE);

        return summaries;
    }

    private StravaActivitySummaryResponse[] requestPage(String accessToken, Instant after, Instant before, int page) {
        try {
            StravaActivitySummaryResponse[] response = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(properties.activitiesUrl())
                            .queryParam("after", after.getEpochSecond())
                            .queryParam("before", before.getEpochSecond())
                            .queryParam("page", page)
                            .queryParam("per_page", PAGE_SIZE)
                            .build()
                            .toUriString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(this::isRateLimited, (request, httpResponse) -> {
                        log.warn("Strava activity summary request rate limited statusCode={}",
                                httpResponse.getStatusCode().value());
                        throw new StravaActivityRateLimitException();
                    })
                    .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                        log.warn("Strava activity summary request failed statusCode={}",
                                httpResponse.getStatusCode().value());
                        throw new StravaActivityUnavailableException();
                    })
                    .body(StravaActivitySummaryResponse[].class);

            if (response == null) {
                return new StravaActivitySummaryResponse[0];
            }

            return response;
        } catch (StravaActivityRateLimitException | StravaActivityUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Strava activity summary request failed category=client_error");
            throw new StravaActivityUnavailableException(exception);
        } catch (RuntimeException exception) {
            log.warn("Strava activity summary response mapping failed");
            throw new StravaActivityUnavailableException(exception);
        }
    }

    private Optional<StravaActivitySummaryImport> toImport(StravaActivitySummaryResponse response) {
        if (response == null || response.id() == null || response.startDate() == null) {
            return Optional.empty();
        }

        return activityType(response.sportType())
                .map(activityType -> new StravaActivitySummaryImport(response.id(), activityType,
                        response.sportType(), response.name(), Instant.parse(response.startDate()),
                        response.distance(), response.movingTime(), response.averageSpeed(),
                        response.totalElevationGain(), response.maxSpeed(), response.averageHeartRate(),
                        response.maxHeartRate(), response.averageCadence(), response.averageWatts(),
                        response.calories()));
    }

    private Optional<StravaActivityType> activityType(String sportType) {
        if (sportType == null) {
            return Optional.empty();
        }

        return switch (sportType) {
            case "Run" -> Optional.of(StravaActivityType.RUN);
            case "Walk" -> Optional.of(StravaActivityType.WALK);
            case "Ride" -> Optional.of(StravaActivityType.RIDE);
            case "WeightTraining" -> Optional.of(StravaActivityType.WEIGHT_TRAINING);
            default -> Optional.empty();
        };
    }

    private boolean isRateLimited(HttpStatusCode statusCode) {
        return statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value();
    }

    private static SimpleClientHttpRequestFactory requestFactory(StravaApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return requestFactory;
    }
}
