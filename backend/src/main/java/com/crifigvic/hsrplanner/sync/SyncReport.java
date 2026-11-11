package com.crifigvic.hsrplanner.sync;

import java.time.Instant;
import java.util.List;

/**
 * @param skipped names HSRWiki sent that we couldn't map (unknown element or path)
 */
public record SyncReport(
        String gameVersion,
        int characters,
        int lightCones,
        int synergies,
        List<String> skipped,
        Instant startedAt,
        Instant finishedAt
) {
}
