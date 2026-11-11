package com.crifigvic.hsrplanner.team.validation;

/** Score for a pair of characters, 0 when they have no known synergy. Order doesn't matter. */
@FunctionalInterface
public interface SynergyLookup {

    int score(int characterId, int otherCharacterId);

    SynergyLookup NONE = (a, b) -> 0;
}
