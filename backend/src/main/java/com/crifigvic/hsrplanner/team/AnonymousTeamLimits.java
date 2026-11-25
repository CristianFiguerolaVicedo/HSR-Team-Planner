package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.common.RateLimiter;
import com.crifigvic.hsrplanner.common.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Brakes on saving teams without an account, so a script can't fill the database (Neon's free plan
 * has 0.5 GB): per visitor, for the whole site per hour, and in total. Logged-in users have their own
 * limit of 200 teams each (TeamPermissions).
 */
@Component
@RequiredArgsConstructor
public class AnonymousTeamLimits {

    /** Plenty for a person sharing a few teams. */
    static final int PER_VISITOR_PER_HOUR = 20;
    /** For everyone together: holds even if a script fakes a different address on every request. */
    static final int ALL_VISITORS_PER_HOUR = 300;
    /** About 100 MB of teams at most; past this, saving without an account stops until some are removed. */
    static final long MAX_STORED = 50_000;

    private final TeamRepository teams;
    private final RateLimiter perVisitor = new RateLimiter(PER_VISITOR_PER_HOUR, Duration.ofHours(1), 10_000);
    private final RateLimiter allVisitors = new RateLimiter(ALL_VISITORS_PER_HOUR, Duration.ofHours(1), 1);

    /** Throws TooManyRequestsException (429) if this anonymous save must wait. */
    public void check(String visitor) {
        if (!perVisitor.tryAcquire(visitor)) {
            throw new TooManyRequestsException("You've shared a lot of teams in the last hour. "
                    + "Wait a while, or log in to save them to your account.");
        }
        if (!allVisitors.tryAcquire("all") || teams.countByOwnerIsNull() >= MAX_STORED) {
            throw new TooManyRequestsException("Saving teams without an account is paused for a while. "
                    + "Log in to save them to your account.");
        }
    }
}
