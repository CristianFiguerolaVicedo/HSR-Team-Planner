package com.crifigvic.hsrplanner.sync;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Repeats a call while it fails with a "try again later" error, until it works or time runs out.
 * Plain Java (clock and sleep are passed in) so the waiting logic can be tested without waiting.
 */
final class Retrier {

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final Duration giveUpAfter;
    private final Duration wait;
    private final Clock clock;
    private final Sleeper sleeper;

    Retrier(Duration giveUpAfter, Duration wait, Clock clock, Sleeper sleeper) {
        this.giveUpAfter = giveUpAfter;
        this.wait = wait;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    static Retrier waitingFor(Duration giveUpAfter, Duration wait) {
        return new Retrier(giveUpAfter, wait, Clock.systemUTC(), d -> Thread.sleep(d.toMillis()));
    }

    /** Runs the action; retries only errors accepted by {@code retryable}. The last error is rethrown. */
    <T> T call(Supplier<T> action, Predicate<RuntimeException> retryable) {
        Instant deadline = clock.instant().plus(giveUpAfter);
        while (true) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                if (!retryable.test(e) || !clock.instant().plus(wait).isBefore(deadline)) {
                    throw e;
                }
                try {
                    sleeper.sleep(wait);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }
}
