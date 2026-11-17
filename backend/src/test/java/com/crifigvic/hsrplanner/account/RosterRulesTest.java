package com.crifigvic.hsrplanner.account;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RosterRulesTest {

    @Test
    void keepsKnownCharactersSortedById() {
        Map<Integer, Integer> clean = RosterRules.sanitize(Map.of(1306, 0, 1102, 2), List.of(1102, 1306, 1208));
        assertEquals(List.of(1102, 1306), List.copyOf(clean.keySet()));
        assertEquals(Integer.valueOf(2), clean.get(1102));
    }

    @Test
    void dropsCharactersTheCatalogDoesntHave() {
        assertEquals(Map.of(1102, 0), RosterRules.sanitize(Map.of(1102, 0, 9999, 3), List.of(1102)));
    }

    @Test
    void emptyRosterIsValid() {
        assertTrue(RosterRules.sanitize(Map.of(), List.of()).isEmpty());
    }

    @Test
    void rejectsEidolonsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> RosterRules.sanitize(Map.of(1102, 7), List.of(1102)));
        assertThrows(IllegalArgumentException.class, () -> RosterRules.sanitize(Map.of(1102, -1), List.of(1102)));
    }

    @Test
    void rejectsMissingValues() {
        Map<Integer, Integer> withNull = new HashMap<>();
        withNull.put(1102, null);
        assertThrows(IllegalArgumentException.class, () -> RosterRules.sanitize(withNull, List.of(1102)));
        assertThrows(IllegalArgumentException.class, () -> RosterRules.sanitize(null, List.of()));
    }

    @Test
    void rejectsHugeRosters() {
        Map<Integer, Integer> huge = new HashMap<>();
        for (int i = 0; i <= RosterRules.MAX_ENTRIES; i++) huge.put(i, 0);
        assertThrows(IllegalArgumentException.class, () -> RosterRules.sanitize(huge, huge.keySet()));
    }
}
