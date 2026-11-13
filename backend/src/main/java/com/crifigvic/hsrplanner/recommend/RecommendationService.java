package com.crifigvic.hsrplanner.recommend;

import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.catalog.CharacterRepository;
import com.crifigvic.hsrplanner.catalog.SynergyLookups;
import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;
import com.crifigvic.hsrplanner.recommend.TeamRecommender.Candidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CharacterRepository characters;
    private final SynergyLookups synergyLookups;
    private final TeamRecommender recommender = new TeamRecommender();

    /**
     * Unknown ids are ignored (an old roster may hold characters the catalog no longer has).
     * Characters in mustInclude that the user doesn't own are still used, at E0.
     */
    @Transactional(readOnly = true)
    public List<RecommendedTeam> recommend(RecommendationRequest request) {
        Map<Integer, Integer> eidolons = new HashMap<>(request.owned());
        request.mustIncludeOrEmpty().forEach(id -> eidolons.putIfAbsent(id, 0));

        Map<Integer, Character> found = characters.findAllById(eidolons.keySet()).stream()
                .collect(Collectors.toMap(Character::getId, Function.identity()));

        List<Candidate> candidates = found.values().stream()
                .sorted(Comparator.comparingInt(Character::getId))
                .map(c -> new Candidate(c.getId(), c.getName(), c.getRoles(), c.getVariantGroup(),
                        Math.clamp(eidolons.getOrDefault(c.getId(), 0), 0, 6)))
                .toList();

        return recommender
                .recommend(candidates, synergyLookups.within(found.keySet()), request.gameMode(),
                        request.mustIncludeOrEmpty(), request.limitOrDefault())
                .stream()
                .map(r -> new RecommendedTeam(
                        r.memberIds().stream().map(found::get).map(CharacterDto::from).toList(),
                        r.memberIds().stream().map(id -> eidolons.getOrDefault(id, 0)).toList(),
                        Math.round(r.score() * 10) / 10.0,
                        r.breakdown(),
                        r.reasons()))
                .toList();
    }
}
