package com.crifigvic.hsrplanner.catalog.dto;

import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.catalog.Element;
import com.crifigvic.hsrplanner.catalog.Path;
import com.crifigvic.hsrplanner.catalog.Role;
import com.crifigvic.hsrplanner.catalog.Tag;

import java.util.List;

public record CharacterDto(
        int id,
        String name,
        int rarity,
        Element element,
        Path path,
        /** Main role, same as {@code roles[0]}. */
        Role role,
        List<Role> roles,
        /** AOE, SINGLE_TARGET, BREAK, in that order. */
        List<Tag> tags,
        double baseSpd,
        String variantGroup,
        String iconUrl,
        String miniIconUrl
) {
    public static CharacterDto from(Character c) {
        return new CharacterDto(c.getId(), c.getName(), c.getRarity(), c.getElement(), c.getPath(), c.mainRole(), List.copyOf(c.getRoles()),
                c.getTags().stream().sorted().toList(),
                c.getBaseSpd(), c.getVariantGroup(), c.getIconUrl(), c.getMiniIconUrl());
    }
}
