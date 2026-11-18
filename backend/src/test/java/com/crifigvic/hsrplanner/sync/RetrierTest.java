package com.crifigvic.hsrplanner.sync;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetrierTest {

    /** A clock that only moves when the fake sleep says so. */
    private static final class FakeTime extends Clock {
        private Instant now = Instant.parse("2026-11-18T19:30:00Z");
        final List<Duration> sleeps = new ArrayList<>();

        void sleep(Duration d) {
            sleeps.add(d);
            now = now.plus(d);
        }

        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    private static final class Transient extends RuntimeException { }
    private static final class Fatal extends RuntimeException { }

    private final FakeTime time = new FakeTime();
    private final Retrier retrier = new Retrier(Duration.ofMinutes(1), Duration.ofSeconds(10), time, time::sleep);

    @Test
    void returnsAtOnceWhenTheFirstCallWorks() {
        assertEquals("ok", retrier.call(() -> "ok", e -> true));
        assertEquals(0, time.sleeps.size());
    }

    @Test
    void retriesTransientErrorsUntilTheCallWorks() {
        AtomicInteger calls = new AtomicInteger();
        String result = retrier.call(() -> {
            if (calls.incrementAndGet() < 3) throw new Transient();
            return "awake";
        }, e -> e instanceof Transient);

        assertEquals("awake", result);
        assertEquals(3, calls.get());
        assertEquals(List.of(Duration.ofSeconds(10), Duration.ofSeconds(10)), time.sleeps);
    }

    @Test
    void doesNotRetryOtherErrors() {
        Fatal fatal = new Fatal();
        AtomicInteger calls = new AtomicInteger();
        RuntimeException thrown = assertThrows(Fatal.class, () -> retrier.call(() -> {
            calls.incrementAndGet();
            throw fatal;
        }, e -> e instanceof Transient));

        assertSame(fatal, thrown);
        assertEquals(1, calls.get());
    }

    @Test
    void givesUpWhenTimeRunsOutAndRethrowsTheLastError() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(Transient.class, () -> retrier.call(() -> {
            calls.incrementAndGet();
            throw new Transient();
        }, e -> e instanceof Transient));

        // 1 minute budget, 10 s waits: calls at 0, 10, 20, 30, 40 and 50 s; one more wait would reach the deadline
        assertEquals(6, calls.get());
        assertEquals(5, time.sleeps.size());
    }
}
