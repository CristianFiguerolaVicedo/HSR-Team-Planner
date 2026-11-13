package com.crifigvic.hsrplanner.importer;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShowcaseCacheTest {

    /** Clock the test moves by hand. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-13T16:00:00Z");

        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    private static ShowcaseSnapshot snapshot(String uid, Duration ttl) {
        return new ShowcaseSnapshot(uid, "x", 70, List.of(), ShowcaseSnapshot.Source.ENKA, ttl);
    }

    @Test
    void keepsASnapshotUntilItsTtlEnds() {
        MutableClock clock = new MutableClock();
        ShowcaseCache cache = new ShowcaseCache(clock);
        cache.put(snapshot("800333171", Duration.ofSeconds(90)));

        clock.advance(Duration.ofSeconds(89));
        assertTrue(cache.get("800333171").isPresent());

        clock.advance(Duration.ofSeconds(1));
        assertTrue(cache.get("800333171").isEmpty());
    }

    @Test
    void reportsWhenEachEntryExpires() {
        MutableClock clock = new MutableClock();
        ShowcaseCache cache = new ShowcaseCache(clock);
        Instant expires = cache.put(snapshot("800333171", Duration.ofSeconds(60)));
        assertEquals(Instant.parse("2026-11-13T16:01:00Z"), expires);
        assertEquals(expires, cache.expiresAt("800333171").orElseThrow());
    }

    @Test
    void neverGrowsPastItsLimit() {
        MutableClock clock = new MutableClock();
        ShowcaseCache cache = new ShowcaseCache(clock);
        for (int i = 0; i < ShowcaseCache.MAX_ENTRIES + 50; i++) {
            cache.put(snapshot(String.valueOf(100_000_000 + i), Duration.ofMinutes(5)));
        }
        long kept = 0;
        for (int i = 0; i < ShowcaseCache.MAX_ENTRIES + 50; i++) {
            if (cache.get(String.valueOf(100_000_000 + i)).isPresent()) kept++;
        }
        assertTrue(kept <= ShowcaseCache.MAX_ENTRIES, "kept " + kept);
        assertTrue(cache.get(String.valueOf(100_000_000 + ShowcaseCache.MAX_ENTRIES + 49)).isPresent(), "newest entry is kept");
    }
}
