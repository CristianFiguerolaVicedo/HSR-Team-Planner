package com.crifigvic.hsrplanner.recommend;

import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;

import java.util.List;

/**
 * One suggested team.
 *
 * @param members  damage dealers first, sustain last
 * @param eidolons Eidolon of each member, same order (from the request's roster)
 * @param score    sum of the breakdown, rounded to one decimal
 */
public record RecommendedTeam(
        List<CharacterDto> members,
        List<Integer> eidolons,
        double score,
        TeamRecommender.Breakdown breakdown,
        List<String> reasons
) {
}
