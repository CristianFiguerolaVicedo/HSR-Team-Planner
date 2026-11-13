package com.crifigvic.hsrplanner.catalog;

/**
 * What a character does in a team. A character can have several (Ashveil is Sub-DPS and Debuffer);
 * the first one is the main role. Every current character is set by hand in
 * {@code catalog/overrides.json}; {@link #defaultFor} only covers characters added later.
 */
public enum Role {
    MAIN_DPS,
    SUB_DPS,
    /** Buffs allies (Robin, Sparkle, Sunday...). */
    AMPLIFIER,
    /** Weakens enemies (Pela, Jiaoqiu, Black Swan...). */
    DEBUFFER,
    SUSTAIN;

    public static Role defaultFor(Path path) {
        return switch (path) {
            case DESTRUCTION, HUNT, ERUDITION -> MAIN_DPS;
            case HARMONY -> AMPLIFIER;
            case NIHILITY -> DEBUFFER;
            case PRESERVATION, ABUNDANCE -> SUSTAIN;
            // Mixed paths: some DPS, some supports
            case REMEMBRANCE, ELATION -> SUB_DPS;
        };
    }
}
