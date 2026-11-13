package com.crifigvic.hsrplanner.team.validation;

import com.crifigvic.hsrplanner.catalog.Role;

import java.util.List;
import java.util.Map;

/**
 * @param valid          false when there's at least one ERROR issue
 * @param roles          how many characters cover each role (one with two roles counts in both)
 * @param synergyPairs   pairs inside the team with a known synergy, best first
 * @param synergyScore   sum of every pair's score
 */
public record TeamValidationResult(
        boolean valid,
        List<ValidationIssue> issues,
        Map<Role, Long> roles,
        List<SynergyPair> synergyPairs,
        int synergyScore
) {
    public record SynergyPair(int characterId, String characterName, int otherCharacterId, String otherCharacterName, int score) {
    }
}
