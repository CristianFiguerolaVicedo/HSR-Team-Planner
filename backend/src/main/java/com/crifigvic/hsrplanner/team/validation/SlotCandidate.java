package com.crifigvic.hsrplanner.team.validation;

import com.crifigvic.hsrplanner.catalog.Path;
import com.crifigvic.hsrplanner.catalog.Role;

import java.util.List;

/**
 * One slot as the validator sees it: plain values, no JPA, so the rules can be tested without a database.
 *
 * @param roles         main role first; a character counts towards each of them
 * @param lightConePath null when the slot has no light cone
 */
public record SlotCandidate(
        int position,
        int characterId,
        String characterName,
        Path path,
        List<Role> roles,
        String variantGroup,
        String lightConeName,
        Path lightConePath
) {
}
