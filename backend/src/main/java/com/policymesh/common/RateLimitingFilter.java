package com.policymesh.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Defensive in-memory rate limiting filter to protect against brute-force attacks,
 * credential stuffing, and volumetric API abuse.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final int AUTH_LIMIT_PER_MINUTE = 30;
  private static final int COMPUTE_LIMIT_PER_MINUTE = 60;
  private static final int GENERAL_LIMIT_PER_MINUTE = 300;
  private static final long WINDOW_MS = 60_000L;

  private final Map<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();
  private volatile long lastCleanup = System.currentTimeMillis();

  private static final class WindowCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    private final long windowStart;

    WindowCounter(long windowStart) {
      this.windowStart = windowStart;
    }

    int incrementAndGet() {
      return count.incrementAndGet();
    }

    int get() {
      return count.get();
    }

    boolean isExpired(long now) {
      return now - windowStart > WINDOW_MS;
    }
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    // Allow health check without throttling
    if ("/health".equals(path) || "/".equals(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    int limit = resolveLimit(path);
    String clientIp = resolveClientIp(request);
    String bucketKey = clientIp + ":" + resolveBucket(path);

    long now = System.currentTimeMillis();
    periodicCleanup(now);

    WindowCounter counter = requestCounts.compute(bucketKey, (k, existing) -> {
      if (existing == null || existing.isExpired(now)) {
        return new WindowCounter(now);
      }
      return existing;
    });

    int currentCount = counter.incrementAndGet();
    int remaining = Math.max(0, limit - currentCount);

    response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
    response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

    if (currentCount > limit) {
      response.setStatus(429);
      response.setHeader("Retry-After", "60");
      response.setContentType("application/problem+json");
      response.getWriter().write("""
          {"type":"https://policymesh/errors/too-many-requests","title":"Too Many Requests","status":429,"detail":"Rate limit exceeded for endpoint. Please retry after 60 seconds.","instance":"%s"}"""
          .formatted(path));
      return;
    }

    filterChain.doFilter(request, response);
  }

  private int resolveLimit(String path) {
    if (path.startsWith("/api/v1/auth/") || path.contains("/change-password")) {
      return AUTH_LIMIT_PER_MINUTE;
    }
    if (path.startsWith("/api/v1/ci/") || path.startsWith("/api/v1/ai/") || path.startsWith("/api/v1/compiler/")) {
      return COMPUTE_LIMIT_PER_MINUTE;
    }
    return GENERAL_LIMIT_PER_MINUTE;
  }

  private String resolveBucket(String path) {
    if (path.startsWith("/api/v1/auth/") || path.contains("/change-password")) {
      return "auth";
    }
    if (path.startsWith("/api/v1/ci/") || path.startsWith("/api/v1/ai/") || path.startsWith("/api/v1/compiler/")) {
      return "compute";
    }
    return "general";
  }

  private String resolveClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      int comma = xForwardedFor.indexOf(',');
      return (comma > 0 ? xForwardedFor.substring(0, comma) : xForwardedFor).trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isBlank()) {
      return xRealIp.trim();
    }
    String remoteAddr = request.getRemoteAddr();
    return remoteAddr != null ? remoteAddr : "127.0.0.1";
  }

  private void periodicCleanup(long now) {
    if (now - lastCleanup > WINDOW_MS * 2) {
      lastCleanup = now;
      requestCounts.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
  }
}
