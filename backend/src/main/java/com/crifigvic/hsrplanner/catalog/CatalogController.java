package com.crifigvic.hsrplanner.catalog;

import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;
import com.crifigvic.hsrplanner.catalog.dto.LightConeDto;
import com.crifigvic.hsrplanner.catalog.dto.SynergyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalog;

    /** {@code GET /api/characters?path=HARMONY&element=ICE&role=SUSTAIN&rarity=5} — every filter is optional. */
    @GetMapping("/characters")
    public List<CharacterDto> characters(@RequestParam(required = false) Path path,
                                         @RequestParam(required = false) Element element,
                                         @RequestParam(required = false) Role role,
                                         @RequestParam(required = false) Integer rarity) {
        return catalog.findCharacters(path, element, role, rarity);
    }

    @GetMapping("/characters/{id}")
    public CharacterDto character(@PathVariable int id) {
        return catalog.getCharacter(id);
    }

    /** Best partners for a character, highest score first. */
    @GetMapping("/characters/{id}/synergies")
    public List<SynergyDto> synergies(@PathVariable int id,
                                      @RequestParam(defaultValue = "10") int limit) {
        return catalog.findSynergies(id, Math.clamp(limit, 1, 50));
    }

    @GetMapping("/lightcones")
    public List<LightConeDto> lightCones(@RequestParam(required = false) Path path,
                                         @RequestParam(required = false) Integer rarity) {
        return catalog.findLightCones(path, rarity);
    }

    @GetMapping("/lightcones/{id}")
    public LightConeDto lightCone(@PathVariable long id) {
        return catalog.getLightCone(id);
    }
}
