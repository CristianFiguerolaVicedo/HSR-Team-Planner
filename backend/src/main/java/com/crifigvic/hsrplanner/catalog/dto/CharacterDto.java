package com.crifigvic.hsrplanner.catalog.dto;

import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.catalog.Element;
import com.crifigvic.hsrplanner.catalog.Path;
import com.crifigvic.hsrplanner.catalog.Role;

public record CharacterDto(
        int id,
        String name,
        int rarity,
        Element element,
        Path path,
        Role role,
        double baseSpd,
        String variantGroup,
        String iconUrl,
        String miniIconUrl
) {
    public static CharacterDto from(Character c) {
        return new CharacterDto(c.getId(), c.getName(), c.getRarity(), c.getElement(), c.getPath(), c.getRole(),
                c.getBaseSpd(), c.getVariantGroup(), c.getIconUrl(), c.getMiniIconUrl());
    }
}
