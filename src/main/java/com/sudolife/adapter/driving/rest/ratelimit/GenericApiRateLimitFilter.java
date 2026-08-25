package com.sudolife.adapter.driving.rest.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitBucketKey;
import com.sudolife.application.service.ratelimit.RateLimitConsumption;
import com.sudolife.application.service.ratelimit.RateLimitPolicy;
import com.sudolife.application.service.ratelimit.ports.required.RateLimitBucketRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class GenericApiRateLimitFilter extends OncePerRequestFilter {

    private static final String API_PATH_PREFIX = "/api/";
    private static final String USER_KEY_PREFIX = "user:";
    private static final String ORIGIN_KEY_PREFIX = "origin:";
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/users/login",
            "/api/users/register",
            "/api/auth/password-recovery",
            "/api/auth/password-recovery/complete",
            "/api/strava/callback",
            "/actuator/health"
    );
    private static final List<String> EXCLUDED_PATH_PREFIXES = List.of(
            "/actuator/health/"
    );
    private static final String RATE_LIMIT_RESPONSE =
            "{\"code\":\"GENERIC_API_RATE_LIMIT_EXCEEDED\",\"message\":\"API rate limit exceeded\"}";

    private final RateLimitBucketRegistry bucketRegistry;
    private final HttpRequestOriginResolver originResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = requestPath(request);

        return !path.startsWith(API_PATH_PREFIX)
                || EXCLUDED_PATHS.contains(path)
                || EXCLUDED_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        RateLimitConsumption consumption = consume(request);
        if (!consumption.allowed()) {
            writeRateLimitResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitConsumption consume(HttpServletRequest request) {
        try {
            return bucketRegistry.consume(bucketKey(request));
        } catch (RuntimeException exception) {
            return RateLimitConsumption.allowedWithoutLimit();
        }
    }

    RateLimitBucketKey bucketKey(HttpServletRequest request) {
        String authenticatedUser = authenticatedUser();
        if (authenticatedUser != null) {
            return new RateLimitBucketKey(RateLimitPolicy.GENERIC_API, USER_KEY_PREFIX + authenticatedUser);
        }

        return new RateLimitBucketKey(RateLimitPolicy.GENERIC_API,
                ORIGIN_KEY_PREFIX + originResolver.resolveOrigin(request));
    }

    private String authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            return null;
        }

        return name.trim();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(RATE_LIMIT_RESPONSE);
    }

    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isBlank()) {
            return uri;
        }
        if (!uri.startsWith(contextPath)) {
            return uri;
        }

        return uri.substring(contextPath.length());
    }
}
