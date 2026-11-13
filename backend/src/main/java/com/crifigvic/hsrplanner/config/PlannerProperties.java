package com.crifigvic.hsrplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** Everything under {@code planner.*} in application.yml. */
@ConfigurationProperties(prefix = "planner")
public record PlannerProperties(Cors cors, HsrWiki hsrwiki, Sync sync, Admin admin, Importer importer, Auth auth) {

    public record Cors(List<String> allowedOrigins) {
    }

    /** @param timeout HSRWiki runs on Render's free plan and can take ~1 min to wake up */
    public record HsrWiki(String baseUrl, Duration timeout) {
    }

    /** @param cron when to re-sync; HSRWiki refreshes its data every Wednesday at 18:00 UTC */
    public record Sync(OnStartup onStartup, String cron) {
    }

    /** @param token required in the X-Admin-Token header; blank disables the admin endpoints */
    public record Admin(String token) {
    }

    /** Import by UID: Enka.Network first, Mihomo as fallback. */
    public record Importer(String enkaUrl, String mihomoUrl, Duration timeout) {
    }

    /** Where the browser goes after an OAuth login succeeds or fails (relative URLs stay on the same site). */
    public record Auth(String successUrl, String failureUrl) {
    }

    public enum OnStartup {ALWAYS, IF_EMPTY, NEVER}
}
