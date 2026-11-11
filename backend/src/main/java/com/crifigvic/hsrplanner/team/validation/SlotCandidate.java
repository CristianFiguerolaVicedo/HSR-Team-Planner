package com.crifigvic.hsrplanner.team.validation;

import com.crifigvic.hsrplanner.catalog.Path;
import com.crifigvic.hsrplanner.catalog.Role;

/**
 * One slot as the validator sees it: plain values, no JPA, so the rules can be tested without a database.
 *
 * @param lightConePath null when the slot has no light cone
 */
public record SlotCandidate(
        int position,
        int characterId,
        String characterName,
        Path path,
        Role role,
        String variantGroup,
        String lightConeName,
        Path lightConePath
) {
}
