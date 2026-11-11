package com.crifigvic.hsrplanner.sync;

import com.crifigvic.hsrplanner.config.PlannerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** When the sync runs on its own: at startup (configurable) and on a weekly cron. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncTriggers {

    private final CatalogSyncService syncService;
    private final PlannerProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        boolean run = switch (properties.sync().onStartup()) {
            case ALWAYS -> true;
            case IF_EMPTY -> syncService.catalogIsEmpty();
            case NEVER -> false;
        };
        if (run) {
            safeSync("startup");
        }
    }

    @Scheduled(cron = "${planner.sync.cron}", zone = "UTC")
    public void weekly() {
        safeSync("schedule");
    }

    /** A failed sync must not take the API down: the previous catalog stays in place. */
    private void safeSync(String trigger) {
        try {
            syncService.sync();
        } catch (RuntimeException e) {
            log.error("Catalog sync ({}) failed, keeping the current catalog", trigger, e);
        }
    }
}
