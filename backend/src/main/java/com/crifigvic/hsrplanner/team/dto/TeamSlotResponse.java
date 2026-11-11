package com.crifigvic.hsrplanner.team.dto;

import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;
import com.crifigvic.hsrplanner.catalog.dto.LightConeDto;
import com.crifigvic.hsrplanner.spd.SpdAnalysis;

/** @param spdAnalysis null when the slot has no SPD */
public record TeamSlotResponse(
        int position,
        CharacterDto character,
        LightConeDto lightCone,
        int eidolon,
        Integer superimposition,
        Double spd,
        SpdAnalysis spdAnalysis
) {
}
