package com.crifigvic.hsrplanner.catalog.dto;

import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.catalog.Synergy;
import com.crifigvic.hsrplanner.catalog.SynergySource;

/** A synergy seen from one character: {@code partner} is the other one. */
public record SynergyDto(CharacterDto partner, int score, SynergySource source, String reason) {

    public static SynergyDto from(Synergy s, int viewpointCharacterId) {
        Character partner = s.getCharacterA().getId() == viewpointCharacterId ? s.getCharacterB() : s.getCharacterA();
        return new SynergyDto(CharacterDto.from(partner), s.getScore(), s.getSource(), s.getReason());
    }
}
