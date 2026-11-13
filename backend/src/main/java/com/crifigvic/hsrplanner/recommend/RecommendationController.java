package com.crifigvic.hsrplanner.recommend;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendations;

    /**
     * Best teams from the user's characters. POST because the roster travels in the body.
     * An empty list means fewer than 4 usable characters, or nobody who deals damage.
     */
    @PostMapping
    public List<RecommendedTeam> recommend(@Valid @RequestBody RecommendationRequest request) {
        return recommendations.recommend(request);
    }
}
