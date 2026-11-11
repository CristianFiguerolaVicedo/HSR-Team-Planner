package com.crifigvic.hsrplanner.team.validation;

import com.crifigvic.hsrplanner.catalog.Path;
import com.crifigvic.hsrplanner.catalog.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamValidatorTest {

    private final TeamValidator validator = new TeamValidator();

    private static SlotCandidate slot(int position, int id, String name, Path path, Role role) {
        return new SlotCandidate(position, id, name, path, role, null, null, null);
    }

    private static final SlotCandidate SEELE = slot(0, 1102, "Seele", Path.HUNT, Role.MAIN_DPS);
    private static final SlotCandidate SPARKLE = slot(1, 1306, "Sparkle", Path.HARMONY, Role.AMPLIFIER);
    private static final SlotCandidate SILVER_WOLF = slot(2, 1006, "Silver Wolf", Path.NIHILITY, Role.AMPLIFIER);
    private static final SlotCandidate FU_XUAN = slot(3, 1208, "Fu Xuan", Path.PRESERVATION, Role.SUSTAIN);

    private static boolean hasIssue(TeamValidationResult result, IssueCode code) {
        return result.issues().stream().anyMatch(i -> i.code() == code);
    }

    @Test
    void balancedTeamIsValidWithoutIssues() {
        TeamValidationResult result = validator.validate(List.of(SEELE, SPARKLE, SILVER_WOLF, FU_XUAN), SynergyLookup.NONE);

        assertTrue(result.valid());
        assertTrue(result.issues().isEmpty());
        assertEquals(1, result.roles().get(Role.MAIN_DPS).longValue());
        assertEquals(2, result.roles().get(Role.AMPLIFIER).longValue());
        assertEquals(1, result.roles().get(Role.SUSTAIN).longValue());
        assertEquals(0, result.roles().get(Role.SUB_DPS).longValue());
    }

    @Test
    void emptyTeamIsAnError() {
        TeamValidationResult result = validator.validate(List.of(), SynergyLookup.NONE);

        assertFalse(result.valid());
        assertTrue(hasIssue(result, IssueCode.EMPTY_TEAM));
    }

    @Test
    void fiveCharactersIsAnError() {
        SlotCandidate fifth = slot(3, 1309, "Robin", Path.HARMONY, Role.AMPLIFIER);
        TeamValidationResult result = validator.validate(List.of(SEELE, SPARKLE, SILVER_WOLF, FU_XUAN, fifth), SynergyLookup.NONE);

        assertFalse(result.valid());
        assertTrue(hasIssue(result, IssueCode.TOO_MANY_SLOTS));
    }

    @Test
    void sameCharacterTwiceIsAnError() {
        SlotCandidate seeleAgain = slot(1, 1102, "Seele", Path.HUNT, Role.MAIN_DPS);
        TeamValidationResult result = validator.validate(List.of(SEELE, seeleAgain), SynergyLookup.NONE);

        assertFalse(result.valid());
        assertTrue(hasIssue(result, IssueCode.DUPLICATE_CHARACTER));
    }

    @Test
    void twoVersionsOfTheSameCharacterIsAnError() {
        SlotCandidate harmonyTb = new SlotCandidate(0, 8006, "Trailblazer (Harmony) · F", Path.HARMONY, Role.AMPLIFIER, "TRAILBLAZER", null, null);
        SlotCandidate preservationTb = new SlotCandidate(1, 8004, "Trailblazer (Preservation) · F", Path.PRESERVATION, Role.SUSTAIN, "TRAILBLAZER", null, null);

        TeamValidationResult result = validator.validate(List.of(harmonyTb, preservationTb), SynergyLookup.NONE);

        assertFalse(result.valid());
        assertTrue(hasIssue(result, IssueCode.DUPLICATE_VARIANT));
    }

    @Test
    void positionUsedTwiceIsAnError() {
        SlotCandidate sparkleOnSeeleSpot = slot(0, 1306, "Sparkle", Path.HARMONY, Role.AMPLIFIER);
        TeamValidationResult result = validator.validate(List.of(SEELE, sparkleOnSeeleSpot), SynergyLookup.NONE);

        assertFalse(result.valid());
        assertTrue(hasIssue(result, IssueCode.DUPLICATE_POSITION));
    }

    @Test
    void lightConeOfAnotherPathIsOnlyAWarning() {
        SlotCandidate seeleWithHarmonyCone = new SlotCandidate(0, 1102, "Seele", Path.HUNT, Role.MAIN_DPS, null,
                "But the Battle Isn't Over", Path.HARMONY);

        TeamValidationResult result = validator.validate(List.of(seeleWithHarmonyCone, SPARKLE, SILVER_WOLF, FU_XUAN), SynergyLookup.NONE);

        assertTrue(result.valid());
        assertTrue(hasIssue(result, IssueCode.LIGHT_CONE_PATH_MISMATCH));
    }

    @Test
    void fullTeamWithoutSustainOrDpsGetsWarnings() {
        List<SlotCandidate> supportsOnly = List.of(
                slot(0, 1306, "Sparkle", Path.HARMONY, Role.AMPLIFIER),
                slot(1, 1309, "Robin", Path.HARMONY, Role.AMPLIFIER),
                slot(2, 1006, "Silver Wolf", Path.NIHILITY, Role.AMPLIFIER),
                slot(3, 1106, "Pela", Path.NIHILITY, Role.AMPLIFIER));

        TeamValidationResult result = validator.validate(supportsOnly, SynergyLookup.NONE);

        assertTrue(result.valid());
        assertTrue(hasIssue(result, IssueCode.NO_SUSTAIN));
        assertTrue(hasIssue(result, IssueCode.NO_DPS));
    }

    @Test
    void roleAdviceWaitsUntilTheTeamIsFull() {
        TeamValidationResult result = validator.validate(List.of(SPARKLE), SynergyLookup.NONE);

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void synergiesAreSummedAndSortedBestFirst() {
        SynergyLookup lookup = (a, b) -> {
            if ((a == 1102 && b == 1306) || (a == 1306 && b == 1102)) return 6;
            if ((a == 1102 && b == 1006) || (a == 1006 && b == 1102)) return 2;
            return 0;
        };

        TeamValidationResult result = validator.validate(List.of(SEELE, SPARKLE, SILVER_WOLF, FU_XUAN), lookup);

        assertEquals(8, result.synergyScore());
        assertEquals(2, result.synergyPairs().size());
        assertEquals(6, result.synergyPairs().getFirst().score());
    }
}
