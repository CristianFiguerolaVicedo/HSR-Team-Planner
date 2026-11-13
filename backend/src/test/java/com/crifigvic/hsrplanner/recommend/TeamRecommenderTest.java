package com.crifigvic.hsrplanner.recommend;

import com.crifigvic.hsrplanner.catalog.Role;
import com.crifigvic.hsrplanner.catalog.Tag;
import com.crifigvic.hsrplanner.recommend.TeamRecommender.Candidate;
import com.crifigvic.hsrplanner.recommend.TeamRecommender.Recommendation;
import com.crifigvic.hsrplanner.team.GameMode;
import com.crifigvic.hsrplanner.team.validation.SynergyLookup;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.crifigvic.hsrplanner.catalog.Role.*;
import static org.junit.jupiter.api.Assertions.*;

class TeamRecommenderTest {

    private final TeamRecommender recommender = new TeamRecommender();

    private static Candidate c(int id, String name, Role... roles) {
        return new Candidate(id, name, List.of(roles), null, 0);
    }

    private static final Candidate SEELE = c(1, "Seele", MAIN_DPS);
    private static final Candidate ACHERON = c(2, "Acheron", MAIN_DPS);
    private static final Candidate SPARKLE = c(3, "Sparkle", AMPLIFIER);
    private static final Candidate PELA = c(4, "Pela", DEBUFFER);
    private static final Candidate SILVER_WOLF = c(5, "Silver Wolf", SUB_DPS);
    private static final Candidate FU_XUAN = c(6, "Fu Xuan", SUSTAIN);
    private static final Candidate GALLAGHER = c(7, "Gallagher", SUSTAIN);
    private static final Candidate JIAOQIU = c(8, "Jiaoqiu", DEBUFFER);

    /** Symmetric synergy table for tests. */
    private static SynergyLookup synergies(int[][] pairs) {
        Map<String, Integer> scores = new HashMap<>();
        for (int[] p : pairs) {
            scores.put(Math.min(p[0], p[1]) + "-" + Math.max(p[0], p[1]), p[2]);
        }
        return (a, b) -> scores.getOrDefault(Math.min(a, b) + "-" + Math.max(a, b), 0);
    }

    private static Set<Integer> ids(Recommendation r) {
        return new HashSet<>(r.memberIds());
    }

    @Test
    void needsAtLeastFourCharacters() {
        List<Recommendation> result = recommender.recommend(List.of(SEELE, SPARKLE, FU_XUAN), SynergyLookup.NONE, null, List.of(), 5);
        assertTrue(result.isEmpty());
    }

    @Test
    void picksTheMostSynergisticPartners() {
        SynergyLookup syn = synergies(new int[][]{{1, 3, 8}, {1, 4, 6}, {1, 6, 4}, {3, 6, 2}});
        List<Candidate> owned = List.of(SEELE, SPARKLE, PELA, FU_XUAN, GALLAGHER, JIAOQIU);

        Recommendation best = recommender.recommend(owned, syn, null, List.of(), 1).getFirst();

        assertEquals(Set.of(1, 3, 4, 6), ids(best));
        assertEquals(8 + 6 + 4 + 2, best.breakdown().synergy());
    }

    @Test
    void prefersASustainOverAnotherSupportWhenSynergiesAreEqual() {
        List<Candidate> owned = List.of(SEELE, SPARKLE, PELA, JIAOQIU, FU_XUAN);

        Recommendation best = recommender.recommend(owned, SynergyLookup.NONE, null, List.of(), 1).getFirst();

        assertTrue(best.memberIds().contains(FU_XUAN.id()));
        assertTrue(best.reasons().stream().anyMatch(r -> r.contains("keeps the team alive")));
    }

    @Test
    void neverPutsTwoVersionsOfTheSameCharacterTogether() {
        Candidate tbHarmony = new Candidate(10, "Trailblazer (Harmony)", List.of(AMPLIFIER), "TRAILBLAZER", 0);
        Candidate tbPreservation = new Candidate(11, "Trailblazer (Preservation)", List.of(SUSTAIN), "TRAILBLAZER", 0);
        SynergyLookup syn = synergies(new int[][]{{1, 10, 9}, {1, 11, 9}, {10, 11, 9}});

        List<Recommendation> result = recommender.recommend(
                List.of(SEELE, tbHarmony, tbPreservation, SPARKLE, PELA), syn, null, List.of(), 5);

        assertFalse(result.isEmpty());
        result.forEach(r -> assertFalse(ids(r).containsAll(Set.of(10, 11)), "two Trailblazers in " + r.memberIds()));
    }

    @Test
    void everyTeamHasSomeoneDealingDamage() {
        List<Candidate> owned = List.of(SPARKLE, PELA, JIAOQIU, FU_XUAN, GALLAGHER, SILVER_WOLF);

        List<Recommendation> result = recommender.recommend(owned, SynergyLookup.NONE, null, List.of(), 5);

        assertFalse(result.isEmpty(), "Silver Wolf is a Sub-DPS, so she can anchor a team");
        result.forEach(r -> assertTrue(r.memberIds().contains(SILVER_WOLF.id())));
    }

    @Test
    void returnsOneTeamPerDamageDealerAtMost() {
        List<Candidate> owned = List.of(SEELE, ACHERON, SPARKLE, PELA, JIAOQIU, FU_XUAN, GALLAGHER);

        List<Recommendation> result = recommender.recommend(owned, SynergyLookup.NONE, null, List.of(), 5);

        assertTrue(result.size() <= 2, "two anchors, so at most two teams");
        assertEquals(result.size(), new HashSet<>(result.stream().map(TeamRecommenderTest::ids).toList()).size());
    }

    @Test
    void mustIncludeIsInEveryResult() {
        List<Candidate> owned = List.of(SEELE, ACHERON, SPARKLE, PELA, JIAOQIU, FU_XUAN, GALLAGHER);

        List<Recommendation> result = recommender.recommend(owned, SynergyLookup.NONE, null, List.of(ACHERON.id(), PELA.id()), 3);

        assertEquals(3, result.size());
        result.forEach(r -> assertTrue(ids(r).containsAll(Set.of(ACHERON.id(), PELA.id()))));
    }

    @Test
    void fixedCharactersThatClashAreRejected() {
        Candidate tbA = new Candidate(10, "TB A", List.of(AMPLIFIER), "TRAILBLAZER", 0);
        Candidate tbB = new Candidate(11, "TB B", List.of(SUSTAIN), "TRAILBLAZER", 0);
        assertThrows(IllegalArgumentException.class, () -> recommender.recommend(
                List.of(SEELE, tbA, tbB, SPARKLE), SynergyLookup.NONE, null, List.of(10, 11), 3));
    }

    @Test
    void pureFictionRewardsTwoDamageDealersAndMemoryOfChaosAHypercarry() {
        List<Candidate> owned = List.of(SEELE, ACHERON, SPARKLE, PELA, FU_XUAN);
        List<Integer> core = List.of(SEELE.id(), FU_XUAN.id());

        Recommendation pf = recommender.recommend(owned, SynergyLookup.NONE, GameMode.PURE_FICTION, core, 1).getFirst();
        Recommendation moc = recommender.recommend(owned, SynergyLookup.NONE, GameMode.MEMORY_OF_CHAOS, core, 1).getFirst();

        assertTrue(pf.memberIds().contains(ACHERON.id()), "dual DPS for Pure Fiction: " + pf.memberIds());
        assertEquals(Set.of(1, 3, 4, 6), ids(moc), "Seele + two supports + sustain for Memory of Chaos");
    }

    @Test
    void eidolonsBreakTies() {
        Candidate pelaE6 = new Candidate(4, "Pela", List.of(DEBUFFER), null, 6);
        List<Candidate> owned = List.of(SEELE, SPARKLE, pelaE6, JIAOQIU, FU_XUAN);

        Recommendation best = recommender.recommend(owned, SynergyLookup.NONE, null, List.of(), 1).getFirst();

        assertTrue(best.memberIds().contains(4));
        assertEquals(3.0, best.breakdown().investment());
    }

    @Test
    void membersAreOrderedDamageFirstSustainLast() {
        Recommendation best = recommender.recommend(List.of(FU_XUAN, PELA, SPARKLE, SEELE), SynergyLookup.NONE, null, List.of(), 1).getFirst();
        assertEquals(List.of(1, 3, 4, 6), best.memberIds());
    }

    @Test
    void resultsAreStableBetweenCalls() {
        List<Candidate> owned = List.of(SEELE, ACHERON, SPARKLE, PELA, JIAOQIU, FU_XUAN, GALLAGHER, SILVER_WOLF);
        List<Recommendation> first = recommender.recommend(owned, SynergyLookup.NONE, null, List.of(), 5);
        List<Recommendation> second = recommender.recommend(new ArrayList<>(owned.reversed()), SynergyLookup.NONE, null, List.of(), 5);
        assertEquals(first.stream().map(Recommendation::memberIds).toList(), second.stream().map(Recommendation::memberIds).toList());
    }

    // --- damage profile tags ---------------------------------------------------------------------

    private static Candidate tagged(int id, String name, Role role, Tag... tags) {
        return new Candidate(id, name, List.of(role), Set.of(tags), null, 0);
    }

    @Test
    void pureFictionPrefersAoeDamageDealers() {
        Candidate aoeDps = tagged(20, "Herta", MAIN_DPS, Tag.AOE);
        Candidate stDps = tagged(21, "Seele", MAIN_DPS, Tag.SINGLE_TARGET);
        List<Candidate> owned = List.of(aoeDps, stDps, SPARKLE, PELA, FU_XUAN);

        Recommendation best = recommender.recommend(owned, SynergyLookup.NONE, GameMode.PURE_FICTION, List.of(), 5).getFirst();

        assertTrue(best.memberIds().contains(20), "AoE carry first in Pure Fiction: " + best.memberIds());
        assertTrue(best.reasons().stream().anyMatch(r -> r.contains("hit many enemies")));
    }

    @Test
    void apocalypticShadowRewardsBreakAndASingleTargetCarry() {
        Candidate boothill = tagged(30, "Boothill", MAIN_DPS, Tag.SINGLE_TARGET, Tag.BREAK);
        Candidate ruanMei = tagged(31, "Ruan Mei", AMPLIFIER, Tag.BREAK);
        Candidate gallagher = tagged(32, "Gallagher", SUSTAIN, Tag.BREAK);
        List<Candidate> owned = List.of(boothill, ruanMei, gallagher, SPARKLE, PELA, FU_XUAN);

        Recommendation best = recommender.recommend(owned, SynergyLookup.NONE, GameMode.APOCALYPTIC_SHADOW, List.of(30), 1).getFirst();

        assertTrue(ids(best).containsAll(Set.of(30, 31, 32)), "break core: " + best.memberIds());
        // break counts twice at most, plus the single-target carry
        assertEquals(TeamRecommender.BREAK_BONUS * 2 + TeamRecommender.SINGLE_TARGET_BONUS,
                best.breakdown().mode() - modeShapeBonus(best), 1e-9);
    }

    @Test
    void tagsDontMatterWithoutAGameMode() {
        Candidate aoeDps = tagged(20, "Herta", MAIN_DPS, Tag.AOE);
        List<Candidate> owned = List.of(aoeDps, SPARKLE, PELA, FU_XUAN);

        Recommendation best = recommender.recommend(owned, SynergyLookup.NONE, null, List.of(), 1).getFirst();

        assertEquals(0, best.breakdown().mode());
    }

    /** The part of the mode score that comes from team shape (dual DPS +3 or nothing in these tests). */
    private static double modeShapeBonus(Recommendation r) {
        return r.reasons().stream().anyMatch(x -> x.startsWith("Two damage dealers")) ? 3 : 0;
    }
}
