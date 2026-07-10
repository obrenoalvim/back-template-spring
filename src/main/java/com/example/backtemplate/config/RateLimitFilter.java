package com.example.backtemplate.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Set<String> LIMITED_PATHS = Set.of("/auth/login", "/auth/register");
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (LIMITED_PATHS.contains(request.getRequestURI())) {
      Bucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(), ip -> newBucket());
      if (!bucket.tryConsume(1)) {
        response.setStatus(429);
        response.setContentType("application/json");
        response
            .getWriter()
            .write(
                "{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\",\"details\":[]}}");
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private Bucket newBucket() {
    return Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build())
        .build();
  }
}
