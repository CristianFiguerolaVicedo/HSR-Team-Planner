package com.crifigvic.hsrplanner.sync;

import com.crifigvic.hsrplanner.common.ForbiddenException;
import com.crifigvic.hsrplanner.config.PlannerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Manual sync, e.g. right after HSRWiki publishes a new game version. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CatalogSyncService syncService;
    private final PlannerProperties properties;

    @PostMapping("/sync")
    public SyncReport sync(@RequestHeader(name = "X-Admin-Token", required = false) String token) {
        requireAdmin(token);
        return syncService.sync();
    }

    private void requireAdmin(String token) {
        String expected = properties.admin().token();
        if (expected == null || expected.isBlank() || token == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Missing or wrong admin token");
        }
    }
}
