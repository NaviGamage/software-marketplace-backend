package com.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory, per-IP rate limiter. Login gets a strict bucket
 * (brute-force protection); every other endpoint gets a looser general
 * bucket (abuse protection). In-memory means limits reset on restart and
 * don't share state across multiple instances — fine for a single-instance
 * deployment, but would need a shared store (e.g., Redis) behind a load balancer.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Bucket newLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newGeneralBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        boolean isLoginEndpoint = request.getRequestURI().equals("/api/auth/login");

        Bucket bucket = isLoginEndpoint
                ? loginBuckets.computeIfAbsent(clientIp, ip -> newLoginBucket())
                : generalBuckets.computeIfAbsent(clientIp, ip -> newGeneralBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            Map<String, Object> body = Map.of(
                    "success", false,
                    "message", isLoginEndpoint
                            ? "Too many login attempts. Please try again in a minute."
                            : "Too many requests. Please slow down."
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}