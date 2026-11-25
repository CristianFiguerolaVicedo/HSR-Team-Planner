package com.crifigvic.hsrplanner.config;

import com.crifigvic.hsrplanner.common.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Limits per visitor on the endpoints that cost the server something: imports by UID (each one is a
 * call to Enka.Network or Mihomo) and the calculations (validate, recommendations, SPD). Normal use
 * never gets near them; a script does. Saving teams without an account has its own limits in
 * TeamController.
 *
 * <p>The visitor is {@code request.getRemoteAddr()}: with server.forward-headers-strategy=framework
 * that is the address Vercel passes on in X-Forwarded-For. Imports also have a limit for the whole
 * server, which no forged header can get around.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter importsPerVisitor = new RateLimiter(10, Duration.ofMinutes(1), 10_000);
    private final RateLimiter importsTotal = new RateLimiter(60, Duration.ofMinutes(1), 1);
    private final RateLimiter calculationsPerVisitor = new RateLimiter(120, Duration.ofMinutes(1), 10_000);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String visitor = request.getRemoteAddr();
        boolean allowed = true;
        if (path.startsWith("/api/import/")) {
            allowed = importsPerVisitor.tryAcquire(visitor) && importsTotal.tryAcquire("all");
        } else if ("POST".equals(request.getMethod())
                && (path.equals("/api/teams/validate") || path.startsWith("/api/recommendations") || path.startsWith("/api/spd"))) {
            allowed = calculationsPerVisitor.tryAcquire(visitor);
        }
        if (!allowed) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Too many requests. Wait a minute and try again.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
