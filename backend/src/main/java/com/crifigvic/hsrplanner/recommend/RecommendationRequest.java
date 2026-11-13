package com.crifigvic.hsrplanner.recommend;

import com.crifigvic.hsrplanner.team.GameMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * @param owned       the user's characters, id → Eidolon (same shape as the frontend's roster)
 * @param gameMode    optional; changes which team shapes score better
 * @param mustInclude optional; characters every team must have (e.g. the slots already filled)
 * @param limit       how many teams to return, 1–10 (5 when missing)
 */
public record RecommendationRequest(
        @NotEmpty @Size(max = 300) Map<Integer, @Min(0) @Max(6) Integer> owned,
        GameMode gameMode,
        @Size(max = TeamRecommender.TEAM_SIZE) List<Integer> mustInclude,
        @Min(1) @Max(10) Integer limit
) {
    public List<Integer> mustIncludeOrEmpty() {
        return mustInclude == null ? List.of() : mustInclude;
    }

    public int limitOrDefault() {
        return limit == null ? 5 : limit;
    }
}
