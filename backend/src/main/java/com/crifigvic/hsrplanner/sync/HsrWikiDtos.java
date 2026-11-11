package com.crifigvic.hsrplanner.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The slice of HSRWiki's JSON the sync needs. Everything else (skills, lore...) is ignored,
 * so HSRWiki can add fields without breaking us.
 */
public final class HsrWikiDtos {

    private HsrWikiDtos() {
    }

    /** {@code GET /api/characters}. Element and path come as enum names: "ICE", "HUNT". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CharacterJson(
            int id,
            String name,
            int rarity,
            String element,
            String path,
            String icon,
            String miniIcon,
            List<AscensionJson> ascension,
            BuildJson build
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AscensionJson(StatJson spd) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatJson(Double base, Double step) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BuildJson(List<TeamJson> teams) {
    }

    /** Members can be "Robin", "Pela / Tribbie" (alternatives) or placeholders like "Any Sustain". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamJson(String name, List<String> teamMembers) {
    }

    /** {@code GET /api/lightcones}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LightConeJson(Integer id, String name, Integer rarity, String path, String icon, String miniIcon) {
    }
}
