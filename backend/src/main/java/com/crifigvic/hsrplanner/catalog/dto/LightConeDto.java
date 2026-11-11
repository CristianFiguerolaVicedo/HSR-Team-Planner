package com.crifigvic.hsrplanner.catalog.dto;

import com.crifigvic.hsrplanner.catalog.LightCone;
import com.crifigvic.hsrplanner.catalog.Path;

public record LightConeDto(long id, String name, int rarity, Path path, String iconUrl) {

    public static LightConeDto from(LightCone lc) {
        return new LightConeDto(lc.getId(), lc.getName(), lc.getRarity(), lc.getPath(), lc.getIconUrl());
    }
}
