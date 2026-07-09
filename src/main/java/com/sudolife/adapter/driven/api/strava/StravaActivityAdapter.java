package com.sudolife.adapter.driven.api.strava;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudolife.adapter.driven.api.strava.dto.StravaActivityDetailResponse;
import com.sudolife.adapter.driven.api.strava.dto.StravaActivityStreamResponse;
import com.sudolife.adapter.driven.api.strava.dto.StravaActivitySummaryResponse;
import com.sudolife.application.model.strava.StravaActivityDetailImport;
import com.sudolife.application.model.strava.StravaActivityStreamImport;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.StravaActivitySummaryImport;
import com.sudolife.application.service.strava.exception.StravaActivityRateLimitException;
import com.sudolife.application.service.strava.exception.StravaActivityStreamUnavailableException;
import com.sudolife.application.service.strava.exception.StravaActivityUnauthorizedException;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;
import com.sudolife.application.service.strava.ports.required.StravaActivityProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StravaActivityAdapter implements StravaActivityProvider {

    private static final int PAGE_SIZE = 200;
    private static final String STREAM_KEYS = "time,distance,velocity_smooth,heartrate,cadence,watts";
    private static final String STREAM_RESOLUTION = "low";
    private static final Set<String> ALLOWED_STREAM_TYPES = Set.of("time", "distance", "velocity_smooth", "heartrate", "cadence", "watts");

    private final StravaApiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public StravaActivityAdapter(StravaApiProperties properties) {
        this(properties, RestClient.builder()
                .requestFactory(requestFactory(properties))
                .build());
    }

    StravaActivityAdapter(StravaApiProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
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

    @Override
    public StravaActivityDetailImport fetchActivityDetail(String accessToken, Long sourceActivityId) {
        try {
            StravaActivityDetailResponse response = requestDetail(accessToken, sourceActivityId);

            return toDetailImport(response).orElseThrow(StravaActivityUnavailableException::new);
        } catch (StravaActivityRateLimitException | StravaActivityUnavailableException |
                 StravaActivityUnauthorizedException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Strava activity detail request failed category=client_error");
            throw new StravaActivityUnavailableException(exception);
        } catch (RuntimeException exception) {
            log.warn("Strava activity detail response mapping failed");
            throw new StravaActivityUnavailableException(exception);
        }
    }

    @Override
    public StravaActivityStreamImport fetchActivityStreams(String accessToken, Long sourceActivityId) {
        try {
            Map<String, StravaActivityStreamResponse> response = requestStreams(accessToken, sourceActivityId);
            List<StravaActivityStreamResponse> availableStreams = response.entrySet().stream()
                    .map(entry -> withType(entry.getKey(), entry.getValue()))
                    .filter(this::isAllowedStream)
                    .toList();

            if (availableStreams.isEmpty()) {
                log.warn("Strava activity stream response has no allowed samples sourceActivityId={} requestedStreamKeys={} allowedStreamTypes={} returnedStreamShape={}",
                        sourceActivityId, STREAM_KEYS, ALLOWED_STREAM_TYPES, streamResponseShape(response));
                throw new StravaActivityStreamUnavailableException("Strava activity stream response has no allowed samples");
            }

            return new StravaActivityStreamImport(availableStreams.stream()
                    .map(stream -> metricName(stream.type()))
                    .toList(), objectMapper.writeValueAsString(availableStreams));
        } catch (StravaActivityRateLimitException | StravaActivityUnavailableException |
                 StravaActivityUnauthorizedException | StravaActivityStreamUnavailableException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            log.warn("Strava activity stream snapshot serialization failed sourceActivityId={} errorMessage={}",
                    sourceActivityId, exception.getMessage());
            throw new StravaActivityUnavailableException(exception);
        } catch (RestClientException exception) {
            log.warn("Strava activity stream request failed sourceActivityId={} exceptionType={} errorMessage={}",
                    sourceActivityId, exception.getClass().getSimpleName(), exception.getMessage());
            throw new StravaActivityUnavailableException(exception);
        } catch (RuntimeException exception) {
            log.warn("Strava activity stream response mapping failed sourceActivityId={} exceptionType={} errorMessage={}",
                    sourceActivityId, exception.getClass().getSimpleName(), exception.getMessage());
            throw new StravaActivityUnavailableException(exception);
        }
    }

    private Map<String, StravaActivityStreamResponse> requestStreams(String accessToken, Long sourceActivityId) {
        Map<String, StravaActivityStreamResponse> response = restClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.activityStreamsUrl())
                        .queryParam("keys", STREAM_KEYS)
                        .queryParam("key_by_type", true)
                        .queryParam("resolution", STREAM_RESOLUTION)
                        .buildAndExpand(sourceActivityId)
                        .toUriString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(this::isRateLimited, (request, httpResponse) -> {
                    log.warn("Strava activity stream request rate limited sourceActivityId={} statusCode={} responseBody={}",
                            sourceActivityId, httpResponse.getStatusCode().value(), responseBody(httpResponse));
                    throw new StravaActivityRateLimitException();
                })
                .onStatus(this::isUnauthorized, (request, httpResponse) -> {
                    log.warn("Strava activity stream request unauthorized sourceActivityId={} statusCode={} responseBody={}",
                            sourceActivityId, httpResponse.getStatusCode().value(), responseBody(httpResponse));
                    throw new StravaActivityUnauthorizedException();
                })
                .onStatus(this::isNotFound, (request, httpResponse) -> {
                    log.warn("Strava activity stream not available sourceActivityId={} statusCode={}",
                            sourceActivityId, httpResponse.getStatusCode().value());
                    throw new StravaActivityStreamUnavailableException("Strava activity stream resource was not found");
                })
                .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                    log.warn("Strava activity stream request failed sourceActivityId={} statusCode={} responseBody={}",
                            sourceActivityId, httpResponse.getStatusCode().value(), responseBody(httpResponse));
                    throw new StravaActivityUnavailableException();
                })
                .body(new org.springframework.core.ParameterizedTypeReference<>() {
                });

        if (response == null) {
            throw new StravaActivityUnavailableException();
        }

        return response;
    }

    private StravaActivityStreamResponse withType(String type, StravaActivityStreamResponse response) {
        if (response == null || response.type() != null) {
            return response;
        }

        return new StravaActivityStreamResponse(type, response.seriesType(), response.resolution(), response.data());
    }

    private StravaActivityDetailResponse requestDetail(String accessToken, Long sourceActivityId) {
        StravaActivityDetailResponse response = restClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.activityDetailUrl())
                        .buildAndExpand(sourceActivityId)
                        .toUriString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(this::isRateLimited, (request, httpResponse) -> {
                    log.warn("Strava activity detail request rate limited statusCode={}",
                            httpResponse.getStatusCode().value());
                    throw new StravaActivityRateLimitException();
                })
                .onStatus(this::isUnauthorized, (request, httpResponse) -> {
                    log.warn("Strava activity detail request unauthorized statusCode={}",
                            httpResponse.getStatusCode().value());
                    throw new StravaActivityUnauthorizedException();
                })
                .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                    log.warn("Strava activity detail request failed statusCode={}",
                            httpResponse.getStatusCode().value());
                    throw new StravaActivityUnavailableException();
                })
                .body(StravaActivityDetailResponse.class);

        if (response == null) {
            throw new StravaActivityUnavailableException();
        }

        return response;
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
                    .onStatus(this::isUnauthorized, (request, httpResponse) -> {
                        log.warn("Strava activity summary request unauthorized statusCode={}",
                                httpResponse.getStatusCode().value());
                        throw new StravaActivityUnauthorizedException();
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
        } catch (StravaActivityRateLimitException | StravaActivityUnavailableException |
                 StravaActivityUnauthorizedException exception) {
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

    private Optional<StravaActivityDetailImport> toDetailImport(StravaActivityDetailResponse response) {
        if (response == null || response.id() == null || response.startDate() == null) {
            return Optional.empty();
        }

        return activityType(response.sportType())
                .map(activityType -> new StravaActivityDetailImport(response.id(), activityType,
                        response.sportType(), response.name(), Instant.parse(response.startDate()),
                        response.distance(), response.movingTime(), response.averageSpeed(),
                        response.totalElevationGain(), response.maxSpeed(), response.averageHeartRate(),
                        response.maxHeartRate(), response.averageCadence(), response.averageWatts(),
                        response.calories()));
    }

    private boolean hasSamples(StravaActivityStreamResponse response) {
        return response != null && response.type() != null && response.data() != null && !response.data().isEmpty();
    }

    private boolean isAllowedStream(StravaActivityStreamResponse response) {
        return hasSamples(response) && ALLOWED_STREAM_TYPES.contains(response.type());
    }

    private String streamResponseShape(Map<String, StravaActivityStreamResponse> response) {
        if (response.isEmpty()) {
            return "empty";
        }

        return response.entrySet().stream()
                .map(this::streamResponseShape)
                .sorted()
                .collect(Collectors.joining(";"));
    }

    private String streamResponseShape(Map.Entry<String, StravaActivityStreamResponse> entry) {
        StravaActivityStreamResponse stream = entry.getValue();

        if (stream == null) {
            return "key=" + entry.getKey() + ",stream=null";
        }

        return "key=" + entry.getKey() + ",type=" + stream.type() + ",seriesType=" + stream.seriesType() +
                ",resolution=" + stream.resolution() + ",samples=" + sampleCount(stream);
    }

    private int sampleCount(StravaActivityStreamResponse stream) {
        if (stream.data() == null) {
            return 0;
        }

        return stream.data().size();
    }

    private String metricName(String type) {
        return switch (type) {
            case "time" -> "time";
            case "distance" -> "distance";
            case "velocity_smooth" -> "velocity";
            case "heartrate" -> "heart_rate";
            case "cadence" -> "cadence";
            case "watts" -> "watts";
            default -> type;
        };
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

    private boolean isUnauthorized(HttpStatusCode statusCode) {
        return statusCode.value() == HttpStatus.UNAUTHORIZED.value();
    }

    private boolean isNotFound(HttpStatusCode statusCode) {
        return statusCode.value() == HttpStatus.NOT_FOUND.value();
    }

    private String responseBody(ClientHttpResponse response) throws IOException {
        return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static SimpleClientHttpRequestFactory requestFactory(StravaApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return requestFactory;
    }
}
