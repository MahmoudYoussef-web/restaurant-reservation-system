package com.mahmoud.reservation.security.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStarts = new ConcurrentHashMap<>();
    private final Map<String, Long> blockTimestamps = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS = 20;
    private static final long TIME_WINDOW_MS = 60_000;
    private static final long BLOCK_DURATION_MS = 120_000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (!isProtectedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(httpRequest);
        long now = System.currentTimeMillis();

        Long blockedUntil = blockTimestamps.get(ip);
        if (blockedUntil != null) {
            if (now < blockedUntil) {
                reject((HttpServletResponse) response, (blockedUntil - now) / 1000);
                return;
            }
            blockTimestamps.remove(ip);
            requestCounts.remove(ip);
            windowStarts.remove(ip);
        }

        long windowStart = windowStarts.computeIfAbsent(ip, k -> now);
        if (now - windowStart >= TIME_WINDOW_MS) {
            windowStarts.put(ip, now);
            requestCounts.remove(ip);
        }

        AtomicInteger counter = requestCounts.computeIfAbsent(ip, k -> new AtomicInteger(0));

        int count = counter.incrementAndGet();
        if (count > MAX_REQUESTS) {
            blockTimestamps.put(ip, now + BLOCK_DURATION_MS);
            requestCounts.remove(ip);
            windowStarts.remove(ip);
            log.warn("Rate limit exceeded for IP {}", ip);
            reject((HttpServletResponse) response, BLOCK_DURATION_MS / 1000);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isProtectedPath(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/forgot-password")
                || path.startsWith("/api/auth/reset-password");
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(Math.max(retryAfterSeconds, 1)));
        response.getWriter().write("{\"message\":\"Too many requests. Try again later.\"}");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
