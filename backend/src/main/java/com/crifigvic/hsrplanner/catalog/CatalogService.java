package com.crifigvic.hsrplanner.catalog;

import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;
import com.crifigvic.hsrplanner.catalog.dto.LightConeDto;
import com.crifigvic.hsrplanner.catalog.dto.SynergyDto;
import com.crifigvic.hsrplanner.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read side of the catalog. The catalog is small (~100 characters, ~170 light cones),
 * so filters run in memory instead of building dynamic queries.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CharacterRepository characters;
    private final LightConeRepository lightCones;
    private final SynergyRepository synergies;

    public List<CharacterDto> findCharacters(Path path, Element element, Role role, Integer rarity) {
        return characters.findAllByOrderByNameAsc().stream()
                .filter(c -> path == null || c.getPath() == path)
                .filter(c -> element == null || c.getElement() == element)
                .filter(c -> role == null || c.getRoles().contains(role))
                .filter(c -> rarity == null || c.getRarity() == rarity)
                .map(CharacterDto::from)
                .toList();
    }

    public CharacterDto getCharacter(int id) {
        return characters.findById(id)
                .map(CharacterDto::from)
                .orElseThrow(() -> new NotFoundException("Character " + id + " not found"));
    }

    public List<SynergyDto> findSynergies(int characterId, int limit) {
        if (!characters.existsById(characterId)) {
            throw new NotFoundException("Character " + characterId + " not found");
        }
        return synergies.findAllInvolving(characterId).stream()
                .limit(limit)
                .map(s -> SynergyDto.from(s, characterId))
                .toList();
    }

    public List<LightConeDto> findLightCones(Path path, Integer rarity) {
        return lightCones.findAllByOrderByRarityDescNameAsc().stream()
                .filter(lc -> path == null || lc.getPath() == path)
                .filter(lc -> rarity == null || lc.getRarity() == rarity)
                .map(LightConeDto::from)
                .toList();
    }

    public LightConeDto getLightCone(long id) {
        return lightCones.findById(id)
                .map(LightConeDto::from)
                .orElseThrow(() -> new NotFoundException("Light cone " + id + " not found"));
    }
}
