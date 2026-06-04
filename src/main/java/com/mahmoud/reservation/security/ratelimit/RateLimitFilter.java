package com.mahmoud.reservation.security.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> blockTimestamps = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS = 10;
    private static final long TIME_WINDOW_MS = 60_000;
    private static final long BLOCK_DURATION_MS = 120_000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (!path.startsWith("/api/auth/login") && !path.startsWith("/api/auth/register")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(httpRequest);
        long now = System.currentTimeMillis();

        Long blockedUntil = blockTimestamps.get(ip);
        if (blockedUntil != null && now < blockedUntil) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"message\":\"Too many requests. Try again later.\"}");
            return;
        }

        if (blockedUntil != null && now >= blockedUntil) {
            blockTimestamps.remove(ip);
            requestCounts.remove(ip);
        }

        AtomicInteger counter = requestCounts.computeIfAbsent(ip, k -> new AtomicInteger(0));

        int count = counter.incrementAndGet();
        if (count > MAX_REQUESTS) {
            blockTimestamps.put(ip, now + BLOCK_DURATION_MS);
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"message\":\"Too many requests. Try again later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
