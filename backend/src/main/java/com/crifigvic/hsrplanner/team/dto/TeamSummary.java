package com.crifigvic.hsrplanner.team.dto;

import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;
import com.crifigvic.hsrplanner.team.GameMode;

import java.time.Instant;
import java.util.List;

/** One row of "My teams": enough to recognise the team, without the full analysis. */
public record TeamSummary(String shareCode, String name, GameMode gameMode, Instant createdAt, Instant updatedAt,
                          List<CharacterDto> members) {
}
