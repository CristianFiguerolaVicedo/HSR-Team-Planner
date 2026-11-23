package com.crifigvic.hsrplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration
@EnableScheduling
public class AppConfig implements WebMvcConfigurer {

    private final PlannerProperties properties;
    private final String publicUrl;

    public AppConfig(PlannerProperties properties, @Value("${PUBLIC_URL:}") String publicUrl) {
        this.properties = properties;
        this.publicUrl = publicUrl;
    }

    /** Injected instead of calling Instant.now() so tests can fix the time. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Behind Vercel's rewrites the backend sees its own host while the browser sends the site's
        // Origin, so writes are checked here: every method the frontend uses must be listed.
        registry.addMapping("/api/**")
                .allowedOrigins(CorsOrigins.of(properties.cors().allowedOrigins(), publicUrl).toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
