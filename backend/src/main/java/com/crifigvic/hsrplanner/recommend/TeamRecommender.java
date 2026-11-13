package com.crifigvic.hsrplanner.recommend;

import com.crifigvic.hsrplanner.catalog.Role;
import com.crifigvic.hsrplanner.catalog.Tag;
import com.crifigvic.hsrplanner.team.GameMode;
import com.crifigvic.hsrplanner.team.validation.SynergyLookup;

import java.util.*;
import java.util.function.Consumer;

/**
 * Rule-based team recommender. Pure Java (no Spring, no JPA) so the rules are unit tested.
 *
 * <p>Search: trying every 4-character combination of ~90 characters is 2.5 million teams, so instead
 * each damage dealer you own is used as an "anchor" and only its best candidates are combined:
 * the {@value #POOL_SIZE} partners with the most synergy, plus the best sustains and supports so the
 * pool can always form a balanced team. That's a few hundred teams per anchor. The best team of each
 * anchor is kept, so results show different cores instead of five variations of the same one.
 * With characters to build around ({@code mustInclude}), those are the anchor.
 *
 * <p>Score = synergy (pairs recommended together in HSRWiki's guides)
 *          + roles (sustain, supports, damage dealers)
 *          + game mode (dual DPS vs hypercarry, AoE for Pure Fiction, break and single target for bosses)
 *          + investment (Eidolons).
 */
public class TeamRecommender {

    public static final int TEAM_SIZE = 4;
    static final int POOL_SIZE = 12;
    /** Extra sustains and supports added to every pool, so a balanced team is always possible. */
    static final int EXTRA_PER_ROLE = 3;
    static final double EIDOLON_WEIGHT = 0.5;
    static final int AOE_BONUS = 2;
    static final int BREAK_BONUS = 2;
    static final int SINGLE_TARGET_BONUS = 1;

    /** A character you own, as the recommender sees it. */
    public record Candidate(int id, String name, List<Role> roles, Set<Tag> tags, String variantGroup, int eidolon) {
        public Candidate {
            roles = List.copyOf(roles);
            tags = Set.copyOf(tags);
        }

        /** Without tags (most supports). */
        public Candidate(int id, String name, List<Role> roles, String variantGroup, int eidolon) {
            this(id, name, roles, Set.of(), variantGroup, eidolon);
        }

        boolean tagged(Tag tag) {
            return tags.contains(tag);
        }

        boolean has(Role role) {
            return roles.contains(role);
        }

        boolean dealsDamage() {
            return has(Role.MAIN_DPS) || has(Role.SUB_DPS);
        }

        boolean supports() {
            return has(Role.AMPLIFIER) || has(Role.DEBUFFER);
        }

        Role mainRole() {
            return roles.isEmpty() ? Role.SUB_DPS : roles.getFirst();
        }
    }

    /** How the score was built, so the frontend can explain it. */
    public record Breakdown(int synergy, double roles, double mode, double investment) {
        double total() {
            return synergy + roles + mode + investment;
        }
    }

    /** @param memberIds in display order: damage dealers first, sustain last */
    public record Recommendation(List<Integer> memberIds, double score, Breakdown breakdown, List<String> reasons) {
    }

    public List<Recommendation> recommend(List<Candidate> owned, SynergyLookup synergies, GameMode mode,
                                          Collection<Integer> mustInclude, int limit) {
        Map<Integer, Candidate> byId = new LinkedHashMap<>();
        owned.forEach(c -> byId.putIfAbsent(c.id(), c));
        if (byId.size() < TEAM_SIZE || limit < 1) {
            return List.of();
        }

        List<Candidate> core = mustInclude.stream().distinct().map(byId::get).filter(Objects::nonNull).toList();
        if (core.size() > TEAM_SIZE) {
            throw new IllegalArgumentException("At most " + TEAM_SIZE + " characters can be fixed");
        }
        if (hasConflict(core)) {
            throw new IllegalArgumentException("The fixed characters can't be in the same team");
        }

        Scorer scorer = new Scorer(synergies, mode);
        if (!core.isEmpty()) {
            List<Recommendation> teams = new ArrayList<>();
            search(core, byId.values(), synergies, team -> teams.add(scorer.score(team)));
            return best(teams, limit);
        }

        List<Candidate> anchors = byId.values().stream().filter(c -> c.has(Role.MAIN_DPS)).toList();
        if (anchors.isEmpty()) {
            anchors = byId.values().stream().filter(c -> c.has(Role.SUB_DPS)).toList();
        }
        // Best team per anchor; different anchors can land on the same 4 characters, so dedupe
        Map<Set<Integer>, Recommendation> bestPerTeam = new HashMap<>();
        for (Candidate anchor : anchors) {
            Recommendation[] best = {null};
            search(List.of(anchor), byId.values(), synergies, team -> {
                Recommendation r = scorer.score(team);
                if (best[0] == null || BY_SCORE.compare(r, best[0]) < 0) best[0] = r;
            });
            if (best[0] != null) {
                bestPerTeam.merge(Set.copyOf(best[0].memberIds()), best[0], (a, b) -> a);
            }
        }
        return best(new ArrayList<>(bestPerTeam.values()), limit);
    }

    // --- search --------------------------------------------------------------------------------

    /** Feeds every valid team = core + combination from the core's pool. */
    private void search(List<Candidate> core, Collection<Candidate> all, SynergyLookup synergies, Consumer<List<Candidate>> sink) {
        int missing = TEAM_SIZE - core.size();
        if (missing == 0) {
            if (isValid(core)) sink.accept(core);
            return;
        }
        List<Candidate> pool = pool(core, all, synergies);
        combinations(pool, missing, 0, new ArrayDeque<>(), extra -> {
            List<Candidate> team = new ArrayList<>(core);
            team.addAll(extra);
            if (isValid(team)) sink.accept(team);
        });
    }

    /** Best partners for the core, plus the best sustains and supports so balance is always reachable. */
    private List<Candidate> pool(List<Candidate> core, Collection<Candidate> all, SynergyLookup synergies) {
        Set<Integer> coreIds = new HashSet<>();
        core.forEach(c -> coreIds.add(c.id()));
        List<Candidate> ranked = all.stream()
                .filter(c -> !coreIds.contains(c.id()) && core.stream().noneMatch(k -> sameCharacter(k, c)))
                .sorted(Comparator.comparingInt((Candidate c) -> -synergyWith(c, core, synergies))
                        .thenComparingInt(c -> -c.eidolon())
                        .thenComparingInt(Candidate::id))
                .toList();

        LinkedHashSet<Candidate> pool = new LinkedHashSet<>(ranked.subList(0, Math.min(POOL_SIZE, ranked.size())));
        ranked.stream().filter(c -> c.has(Role.SUSTAIN)).limit(EXTRA_PER_ROLE).forEach(pool::add);
        ranked.stream().filter(Candidate::supports).limit(EXTRA_PER_ROLE).forEach(pool::add);
        return List.copyOf(pool);
    }

    private static int synergyWith(Candidate c, List<Candidate> core, SynergyLookup synergies) {
        return core.stream().mapToInt(k -> synergies.score(k.id(), c.id())).sum();
    }

    private static void combinations(List<Candidate> pool, int size, int from, Deque<Candidate> current,
                                     Consumer<List<Candidate>> sink) {
        if (current.size() == size) {
            sink.accept(List.copyOf(current));
            return;
        }
        for (int i = from; i <= pool.size() - (size - current.size()); i++) {
            current.addLast(pool.get(i));
            combinations(pool, size, i + 1, current, sink);
            current.removeLast();
        }
    }

    /** Game rules plus "someone has to deal damage". */
    private static boolean isValid(List<Candidate> team) {
        return team.size() == TEAM_SIZE && !hasConflict(team) && team.stream().anyMatch(Candidate::dealsDamage);
    }

    private static boolean hasConflict(List<Candidate> team) {
        for (int i = 0; i < team.size(); i++) {
            for (int j = i + 1; j < team.size(); j++) {
                if (team.get(i).id() == team.get(j).id() || sameCharacter(team.get(i), team.get(j))) return true;
            }
        }
        return false;
    }

    private static boolean sameCharacter(Candidate a, Candidate b) {
        return a.variantGroup() != null && a.variantGroup().equals(b.variantGroup());
    }

    // --- ranking -------------------------------------------------------------------------------

    /** Higher score first; ties broken by ids so results never shuffle between calls. */
    static final Comparator<Recommendation> BY_SCORE = Comparator
            .comparingDouble((Recommendation r) -> -r.score())
            .thenComparing(r -> r.memberIds().stream().sorted().toList().toString());

    private static List<Recommendation> best(List<Recommendation> teams, int limit) {
        return teams.stream().sorted(BY_SCORE).limit(limit).toList();
    }

    /** Applies the rules to one team and writes the reasons a player would read. */
    private static final class Scorer {

        private static final List<Role> DISPLAY_ORDER =
                List.of(Role.MAIN_DPS, Role.SUB_DPS, Role.AMPLIFIER, Role.DEBUFFER, Role.SUSTAIN);

        private final SynergyLookup synergies;
        private final GameMode mode;

        Scorer(SynergyLookup synergies, GameMode mode) {
            this.synergies = synergies;
            this.mode = mode;
        }

        Recommendation score(List<Candidate> team) {
            List<String> reasons = new ArrayList<>();

            // Synergy: every pair, best pairs explained
            int synergy = 0;
            List<String> pairReasons = new ArrayList<>();
            record Pair(Candidate a, Candidate b, int score) {}
            List<Pair> pairs = new ArrayList<>();
            for (int i = 0; i < team.size(); i++) {
                for (int j = i + 1; j < team.size(); j++) {
                    int s = synergies.score(team.get(i).id(), team.get(j).id());
                    synergy += s;
                    if (s > 0) pairs.add(new Pair(team.get(i), team.get(j), s));
                }
            }
            pairs.sort(Comparator.comparingInt(Pair::score).reversed());
            pairs.stream().limit(2).forEach(p ->
                    pairReasons.add(p.a().name() + " + " + p.b().name() + " are recommended together (+" + p.score() + ")"));
            reasons.addAll(pairReasons);

            // Roles
            long sustains = team.stream().filter(c -> c.has(Role.SUSTAIN)).count();
            long supports = team.stream().filter(Candidate::supports).count();
            long damage = team.stream().filter(Candidate::dealsDamage).count();
            long mains = team.stream().filter(c -> c.has(Role.MAIN_DPS)).count();

            double roles = 0;
            if (sustains == 0) {
                roles -= 6;
                reasons.add("No sustain: risky in harder fights");
            } else if (sustains == 1) {
                roles += 4;
                reasons.add(nameWith(team, Role.SUSTAIN) + " keeps the team alive");
            } else {
                roles += 1;
            }
            roles += 2 * Math.min(supports, 2);

            // Game mode: team shape
            double modeScore = 0;
            if (mode == GameMode.PURE_FICTION || mode == GameMode.APOCALYPTIC_SHADOW) {
                if (damage >= 2) {
                    modeScore += 3;
                    reasons.add("Two damage dealers suit " + label(mode));
                }
            } else if (mode == GameMode.MEMORY_OF_CHAOS || mode == GameMode.ANOMALY_ARBITRATION) {
                if (mains == 1 && supports >= 2) {
                    modeScore += 3;
                    reasons.add("Hypercarry (one DPS, two supports) suits " + label(mode));
                } else if (mains >= 2) {
                    modeScore -= 2;
                }
            } else if (mode == GameMode.OVERWORLD && sustains >= 1) {
                modeScore += 2;
            }

            // Game mode: damage profile (tags)
            Candidate carry = carry(team);
            if (mode == GameMode.PURE_FICTION) {
                List<String> aoe = team.stream().filter(c -> c.dealsDamage() && c.tagged(Tag.AOE)).map(Candidate::name).limit(2).toList();
                if (!aoe.isEmpty()) {
                    modeScore += AOE_BONUS * aoe.size();
                    reasons.add(String.join(" and ", aoe) + " hit many enemies at once (Pure Fiction)");
                }
            } else if (mode == GameMode.APOCALYPTIC_SHADOW) {
                List<String> breakers = team.stream().filter(c -> c.tagged(Tag.BREAK)).map(Candidate::name).limit(2).toList();
                if (!breakers.isEmpty()) {
                    modeScore += BREAK_BONUS * breakers.size();
                    reasons.add(String.join(" and ", breakers) + " bring break for the boss (Apocalyptic Shadow)");
                }
            }
            if ((mode == GameMode.APOCALYPTIC_SHADOW || mode == GameMode.MEMORY_OF_CHAOS)
                    && carry != null && carry.tagged(Tag.SINGLE_TARGET)) {
                modeScore += SINGLE_TARGET_BONUS;
                reasons.add(carry.name() + " focuses single targets (" + label(mode) + ")");
            }

            // Investment: Eidolons you already have
            double investment = EIDOLON_WEIGHT * team.stream().mapToInt(Candidate::eidolon).sum();
            if (investment > 0) {
                reasons.add("Uses your Eidolons (+" + format(investment) + ")");
            }

            Breakdown breakdown = new Breakdown(synergy, roles, modeScore, investment);
            List<Integer> ordered = team.stream()
                    .sorted(Comparator.comparingInt((Candidate c) -> DISPLAY_ORDER.indexOf(c.mainRole())))
                    .map(Candidate::id)
                    .toList();
            return new Recommendation(ordered, breakdown.total(), breakdown, List.copyOf(reasons));
        }

        /** Who carries the damage: the first Main DPS, or the first Sub-DPS without one. */
        private static Candidate carry(List<Candidate> team) {
            return team.stream().filter(c -> c.has(Role.MAIN_DPS)).findFirst()
                    .or(() -> team.stream().filter(c -> c.has(Role.SUB_DPS)).findFirst())
                    .orElse(null);
        }

        private static String nameWith(List<Candidate> team, Role role) {
            return team.stream().filter(c -> c.has(role)).map(Candidate::name).findFirst().orElse("");
        }

        private static String label(GameMode mode) {
            String words = mode.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            StringBuilder out = new StringBuilder();
            for (String w : words.split(" ")) {
                if (!out.isEmpty()) out.append(' ');
                out.append(w.equals("of") ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1));
            }
            return out.toString();
        }

        private static String format(double value) {
            return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
        }
    }
}
