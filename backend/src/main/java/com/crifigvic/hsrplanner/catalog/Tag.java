package com.crifigvic.hsrplanner.catalog;

/**
 * Damage profile of a character, set by hand in {@code catalog/overrides.json}.
 * Used by the recommender to match teams to game modes.
 */
public enum Tag {
    /** Strong damage against many enemies: good for Pure Fiction. */
    AOE,
    /** Best against one enemy (bosses): good for Apocalyptic Shadow and Memory of Chaos. */
    SINGLE_TARGET,
    /** Toughness break / break damage: good for Apocalyptic Shadow. */
    BREAK
}
