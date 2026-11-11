package com.crifigvic.hsrplanner.catalog;

/**
 * What a character does in a team. The sync gives a default from the Path
 * and {@code catalog/overrides.json} corrects the exceptions (Kafka is Nihility but a DPS...).
 */
public enum Role {
    MAIN_DPS,
    SUB_DPS,
    AMPLIFIER,
    SUSTAIN;

    public static Role defaultFor(Path path) {
        return switch (path) {
            case DESTRUCTION, HUNT, ERUDITION -> MAIN_DPS;
            case HARMONY, NIHILITY -> AMPLIFIER;
            case PRESERVATION, ABUNDANCE -> SUSTAIN;
            // Mixed paths: some DPS, some supports. Overrides fix each character.
            case REMEMBRANCE, ELATION -> SUB_DPS;
        };
    }
}
