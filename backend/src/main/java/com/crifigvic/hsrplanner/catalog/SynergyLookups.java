package com.crifigvic.hsrplanner.catalog;

import com.crifigvic.hsrplanner.team.validation.SynergyLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Builds a {@link SynergyLookup} for a set of characters with a single query. */
@Component
@RequiredArgsConstructor
public class SynergyLookups {

    private final SynergyRepository synergies;

    /** Every pair inside {@code characterIds}; CURATED and BUILD_GUIDE scores add up. */
    @Transactional(readOnly = true)
    public SynergyLookup within(Collection<Integer> characterIds) {
        Map<Long, Integer> scores = new HashMap<>();
        for (Synergy s : synergies.findAllWithin(characterIds)) {
            scores.merge(pairKey(s.getCharacterA().getId(), s.getCharacterB().getId()), s.getScore(), Integer::sum);
        }
        return (a, b) -> scores.getOrDefault(pairKey(a, b), 0);
    }

    private static long pairKey(int a, int b) {
        return ((long) Math.min(a, b) << 32) | Math.max(a, b);
    }
}
