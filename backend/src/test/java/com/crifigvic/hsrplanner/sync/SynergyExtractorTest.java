package com.crifigvic.hsrplanner.sync;

import com.crifigvic.hsrplanner.sync.HsrWikiDtos.BuildJson;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.CharacterJson;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.TeamJson;
import com.crifigvic.hsrplanner.sync.SynergyExtractor.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SynergyExtractorTest {

    private static CharacterJson character(int id, String name, TeamJson... teams) {
        return new CharacterJson(id, name, 5, "QUANTUM", "HUNT", null, null, List.of(),
                new BuildJson(List.of(teams)));
    }

    @Test
    void fixedMembersScoreTwoAndAlternativesScoreOne() {
        CharacterJson seele = character(1102, "Seele",
                new TeamJson("Hypercarry", List.of("Seele", "Sparkle", "Pela / Silver Wolf", "Any Sustain")));
        List<CharacterJson> all = List.of(seele,
                character(1306, "Sparkle"), character(1106, "Pela"), character(1006, "Silver Wolf"));

        CharacterNames names = new CharacterNames();
        all.forEach(c -> names.register(c.name(), c.id()));

        Map<Pair, SynergyExtractor.Score> result = new SynergyExtractor().extract(all, names);

        assertEquals(2, result.get(Pair.of(1102, 1306)).value());
        assertEquals(1, result.get(Pair.of(1102, 1106)).value());
        assertEquals(1, result.get(Pair.of(1102, 1006)).value());
        assertEquals(3, result.size(), "the owner and 'Any Sustain' don't create pairs");
        assertTrue(result.get(Pair.of(1102, 1306)).reason().contains("Hypercarry"));
    }

    @Test
    void bothDirectionsAddUpOnTheSamePair() {
        List<CharacterJson> all = List.of(
                character(1102, "Seele", new TeamJson("A", List.of("Sparkle"))),
                character(1306, "Sparkle", new TeamJson("B", List.of("Seele"))));
        CharacterNames names = new CharacterNames();
        all.forEach(c -> names.register(c.name(), c.id()));

        Map<Pair, SynergyExtractor.Score> result = new SynergyExtractor().extract(all, names);

        assertEquals(1, result.size());
        assertEquals(4, result.get(Pair.of(1306, 1102)).value());
    }

    @Test
    void trailblazerShortNameMatchesBothVersionsButNotItself() {
        CharacterJson tbM = character(8005, "TrailblazerHarmony#M", new TeamJson("Break", List.of("Trailblazer (Harmony)", "Firefly")));
        CharacterJson tbF = character(8006, "TrailblazerHarmony#F");
        CharacterJson firefly = character(1310, "Firefly", new TeamJson("Break", List.of("Trailblazer (Harmony)")));
        List<CharacterJson> all = List.of(tbM, tbF, firefly);
        CharacterNames names = new CharacterNames();
        all.forEach(c -> names.register(c.name(), c.id()));

        Map<Pair, SynergyExtractor.Score> result = new SynergyExtractor().extract(all, names);

        assertNull(result.get(Pair.of(8005, 8006)), "M and F are the same character");
        assertEquals(4, result.get(Pair.of(1310, 8005)).value());
        assertEquals(2, result.get(Pair.of(1310, 8006)).value());
    }

    @Test
    void displayNameMakesTrailblazersReadable() {
        assertEquals("Trailblazer (Remembrance) · F", CharacterNames.displayName("TrailblazerRemembrance#F"));
        assertEquals("Dan Heng • Imbibitor Lunae", CharacterNames.displayName("Dan Heng • Imbibitor Lunae"));
    }

    @Test
    void aliasesResolveShortNames() {
        CharacterNames names = new CharacterNames();
        names.register("Topaz & Numby", 1112);

        assertTrue(names.addAlias("Topaz", "Topaz & Numby"));
        assertEquals(Set.of(1112), names.resolve("topaz"));
        assertFalse(names.addAlias("Nobody", "Unknown character"));
    }
}
