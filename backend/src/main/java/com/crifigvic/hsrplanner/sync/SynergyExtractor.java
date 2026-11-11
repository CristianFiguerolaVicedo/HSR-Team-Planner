package com.crifigvic.hsrplanner.sync;

import com.crifigvic.hsrplanner.sync.HsrWikiDtos.CharacterJson;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.TeamJson;

import java.util.*;

/**
 * Builds pair synergies from HSRWiki's recommended teams.
 * For a character X and each recommended team of X:
 * a fixed member Y adds {@value #FIXED_MEMBER_SCORE} to (X, Y);
 * each option of "A / B" adds {@value #ALTERNATIVE_SCORE} to (X, A) and (X, B).
 */
public class SynergyExtractor {

    static final int FIXED_MEMBER_SCORE = 2;
    static final int ALTERNATIVE_SCORE = 1;
    private static final int MAX_REASON_LENGTH = 500;

    /** Pair with {@code lowId < highId}. */
    public record Pair(int lowId, int highId) {
        static Pair of(int a, int b) {
            return new Pair(Math.min(a, b), Math.max(a, b));
        }
    }

    public static final class Score {
        private int value;
        private final Set<String> teams = new LinkedHashSet<>();

        public int value() {
            return value;
        }

        public String reason() {
            String joined = "Recommended together in: " + String.join(", ", teams);
            return joined.length() <= MAX_REASON_LENGTH ? joined : joined.substring(0, MAX_REASON_LENGTH - 1) + "…";
        }
    }

    public Map<Pair, Score> extract(List<CharacterJson> characters, CharacterNames names) {
        Map<Pair, Score> scores = new LinkedHashMap<>();
        for (CharacterJson owner : characters) {
            if (owner.build() == null || owner.build().teams() == null) {
                continue;
            }
            for (TeamJson team : owner.build().teams()) {
                if (team.teamMembers() == null) {
                    continue;
                }
                for (String member : team.teamMembers()) {
                    String[] options = member.split("\\s+/\\s+");
                    int points = options.length == 1 ? FIXED_MEMBER_SCORE : ALTERNATIVE_SCORE;
                    for (String option : options) {
                        for (int partnerId : names.resolve(option)) {
                            if (!names.sameCharacter(owner.id(), partnerId)) {
                                Score score = scores.computeIfAbsent(Pair.of(owner.id(), partnerId), p -> new Score());
                                score.value += points;
                                score.teams.add(team.name() == null ? CharacterNames.displayName(owner.name()) : team.name());
                            }
                        }
                    }
                }
            }
        }
        return scores;
    }
}
