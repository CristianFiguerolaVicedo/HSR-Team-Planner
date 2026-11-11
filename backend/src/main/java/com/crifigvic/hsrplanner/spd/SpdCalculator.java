package com.crifigvic.hsrplanner.spd;

import java.util.ArrayList;
import java.util.List;

/**
 * Turn-order maths for Honkai: Star Rail, without Spring so it can be unit tested on its own.
 *
 * <ul>
 *   <li>A unit acts every {@code 10000 / SPD} action value (AV).</li>
 *   <li>The first cycle (cycle 0) lasts 150 AV; every later cycle lasts 100 AV.</li>
 *   <li>An action that lands exactly on the end of a cycle still counts for that cycle.</li>
 * </ul>
 *
 * Actions are counted cumulatively: with {@code T} AV elapsed at the end of cycle {@code n},
 * a unit has acted {@code floor(T * SPD / 10000)} times. That gives the usual community
 * breakpoints: 133.34 SPD for 2 actions in cycle 0, 160 for 4 actions in two cycles, etc.
 *
 * Out of scope for the MVP: action advance/delay (Bronya, Sunday, Dance Dance Dance...)
 * and SPD buffs that change during the fight.
 */
public final class SpdCalculator {

    public static final double AV_CONSTANT = 10_000;
    public static final double FIRST_CYCLE_AV = 150;
    public static final double CYCLE_AV = 100;

    public static final double MIN_SPD = 1;
    public static final double MAX_SPD = 1_000;
    public static final int MAX_CYCLES = 30;

    /** Absorbs floating point noise so a SPD sitting exactly on a breakpoint counts as reaching it. */
    private static final double EPSILON = 1e-9;

    /** AV between two consecutive actions of a unit with this SPD. */
    public double actionValue(double spd) {
        requireValidSpd(spd);
        return AV_CONSTANT / spd;
    }

    /** Total AV elapsed when cycle {@code cycle} (0-based) ends: 150, 250, 350... */
    public double cycleEndAv(int cycle) {
        requireValidCycle(cycle);
        return FIRST_CYCLE_AV + CYCLE_AV * cycle;
    }

    /** Actions taken from the start of the fight until the end of cycle {@code cycle}, both included. */
    public int actionsUntilEndOfCycle(double spd, int cycle) {
        requireValidSpd(spd);
        return (int) Math.floor(cycleEndAv(cycle) * spd / AV_CONSTANT + EPSILON);
    }

    /** Actions taken inside cycle {@code cycle} only. */
    public int actionsInCycle(double spd, int cycle) {
        int until = actionsUntilEndOfCycle(spd, cycle);
        return cycle == 0 ? until : until - actionsUntilEndOfCycle(spd, cycle - 1);
    }

    /** Minimum SPD needed to have taken {@code actions} actions by the end of cycle {@code cycle}. */
    public double breakpoint(int actions, int cycle) {
        if (actions < 1) {
            throw new IllegalArgumentException("actions must be at least 1, got " + actions);
        }
        return actions * AV_CONSTANT / cycleEndAv(cycle);
    }

    /** The next breakpoint above {@code spd}: one more action by the end of {@code cycle}. */
    public double nextBreakpoint(double spd, int cycle) {
        return breakpoint(actionsUntilEndOfCycle(spd, cycle) + 1, cycle);
    }

    /** SPD still missing to reach {@link #nextBreakpoint}. Always greater than 0. */
    public double spdToNextBreakpoint(double spd, int cycle) {
        return nextBreakpoint(spd, cycle) - spd;
    }

    /** Everything the frontend needs for one unit, for cycles 0 to {@code cycles - 1}. */
    public SpdAnalysis analyze(double spd, int cycles) {
        requireValidSpd(spd);
        if (cycles < 1 || cycles > MAX_CYCLES) {
            throw new IllegalArgumentException("cycles must be between 1 and " + MAX_CYCLES + ", got " + cycles);
        }
        List<SpdAnalysis.CycleBreakdown> breakdown = new ArrayList<>(cycles);
        for (int cycle = 0; cycle < cycles; cycle++) {
            breakdown.add(new SpdAnalysis.CycleBreakdown(
                    cycle,
                    actionsInCycle(spd, cycle),
                    actionsUntilEndOfCycle(spd, cycle),
                    nextBreakpoint(spd, cycle),
                    spdToNextBreakpoint(spd, cycle)));
        }
        return new SpdAnalysis(spd, actionValue(spd), breakdown);
    }

    private static void requireValidSpd(double spd) {
        if (!Double.isFinite(spd) || spd < MIN_SPD || spd > MAX_SPD) {
            throw new IllegalArgumentException("SPD must be between " + MIN_SPD + " and " + MAX_SPD + ", got " + spd);
        }
    }

    private static void requireValidCycle(int cycle) {
        if (cycle < 0 || cycle >= MAX_CYCLES) {
            throw new IllegalArgumentException("cycle must be between 0 and " + (MAX_CYCLES - 1) + ", got " + cycle);
        }
    }
}
