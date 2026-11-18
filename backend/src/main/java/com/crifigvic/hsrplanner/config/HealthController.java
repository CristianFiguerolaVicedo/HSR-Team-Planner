package com.crifigvic.hsrplanner.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Cheap "I'm up" answer for Render's health check and the weekly sync workflow.
 * It doesn't touch the database, so it answers as soon as the app has started.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
