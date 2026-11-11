package com.crifigvic.hsrplanner.spd;

import java.util.List;

/**
 * Result of {@link SpdCalculator#analyze}. Cycle 0 is the first entry of {@code cycles}.
 *
 * @param actionValue AV between two actions of this unit
 */
public record SpdAnalysis(double spd, double actionValue, List<CycleBreakdown> cycles) {

    /**
     * @param cycle            0-based cycle index
     * @param actions          actions taken inside this cycle
     * @param totalActions     actions taken from the start of the fight until the end of this cycle
     * @param nextBreakpoint   SPD needed for one more action by the end of this cycle
     * @param spdMissing       {@code nextBreakpoint - spd}
     */
    public record CycleBreakdown(int cycle, int actions, int totalActions, double nextBreakpoint, double spdMissing) {
    }

    /** Shortcut for the most asked question: actions in cycle 0. */
    public CycleBreakdown cycleZero() {
        return cycles.getFirst();
    }
}
