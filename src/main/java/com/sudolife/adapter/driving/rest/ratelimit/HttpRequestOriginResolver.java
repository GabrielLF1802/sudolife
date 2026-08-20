package com.sudolife.adapter.driving.rest.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestOriginResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN_ORIGIN = "unknown";

    public String resolveOrigin(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return firstForwardedOrigin(forwardedFor);
        }

        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return UNKNOWN_ORIGIN;
        }

        return remoteAddress.trim();
    }

    private String firstForwardedOrigin(String forwardedFor) {
        return forwardedFor.split(",")[0].trim();
    }
}
