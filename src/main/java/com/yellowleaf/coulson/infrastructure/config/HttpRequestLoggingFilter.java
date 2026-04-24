package com.yellowleaf.coulson.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    private final RequestLoggingProperties properties;

    public HttpRequestLoggingFilter(RequestLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }

        String uri = request.getRequestURI();
        for (String prefix : properties.getIgnorePrefixes()) {
            if (uri != null && uri.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        long startNs = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            int status = response.getStatus();

            if (query != null && !query.isBlank()) {
                log.info("HTTP {} {}?{} status={} durationMs={} requestId={}",
                        method, uri, query, status, elapsedMs, requestId);
            } else {
                log.info("HTTP {} {} status={} durationMs={} requestId={}",
                        method, uri, status, elapsedMs, requestId);
            }

            MDC.remove(MDC_KEY);
        }
    }
}

