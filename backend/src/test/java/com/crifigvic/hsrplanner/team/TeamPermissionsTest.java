package com.crifigvic.hsrplanner.team;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamPermissionsTest {

    @Test
    void theOwnerCanModify() {
        assertTrue(TeamPermissions.canModify(7L, 7L));
    }

    @Test
    void someoneElseCannot() {
        assertFalse(TeamPermissions.canModify(7L, 8L));
    }

    @Test
    void nobodyCanModifyATeamSavedWithoutAnAccount() {
        assertFalse(TeamPermissions.canModify(null, 8L));
        assertFalse(TeamPermissions.canModify(null, null));
    }

    @Test
    void comparesValuesNotReferences() {
        // Long values above 127 aren't cached: == would wrongly say "different user"
        assertTrue(TeamPermissions.canModify(Long.valueOf(100_000L), Long.valueOf(100_000L)));
    }
}
