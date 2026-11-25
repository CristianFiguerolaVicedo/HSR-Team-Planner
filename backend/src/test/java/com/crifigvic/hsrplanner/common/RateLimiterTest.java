package com.crifigvic.hsrplanner.common;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    /** A clock the test moves by hand. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-25T12:00:00Z");

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void allowsUpToTheLimitPerWindowThenAgainInTheNextOne() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), 100, clock);
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("b"), "each key has its own count");

        clock.now = clock.now.plus(Duration.ofMinutes(1));
        assertTrue(limiter.tryAcquire("a"), "a new window starts over");
    }

    @Test
    void manyDifferentKeysDontGrowWithoutLimit() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), 10, clock);
        for (int i = 0; i < 1_000; i++) {
            assertTrue(limiter.tryAcquire("visitor-" + i));
        }
        // The table started over along the way instead of keeping 1,000 keys; new keys still work
        assertTrue(limiter.tryAcquire("someone-new"));
    }
}
