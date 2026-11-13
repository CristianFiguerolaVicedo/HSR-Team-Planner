package com.crifigvic.hsrplanner.importer;

import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;

import java.time.Instant;
import java.util.List;

/**
 * Answer of {@code GET /api/import/{uid}}.
 *
 * @param unknownAvatarIds characters in the showcase that the catalog doesn't have yet (just released)
 * @param refreshAfter     asking again before this returns the same snapshot
 */
public record ImportResult(
        String uid,
        String nickname,
        Integer level,
        ShowcaseSnapshot.Source source,
        List<ImportedCharacter> characters,
        List<Integer> unknownAvatarIds,
        Instant refreshAfter
) {
    public record ImportedCharacter(CharacterDto character, int eidolon, Integer level) {
    }
}
