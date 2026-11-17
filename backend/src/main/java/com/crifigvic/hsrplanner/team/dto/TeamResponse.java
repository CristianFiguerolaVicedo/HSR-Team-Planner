package com.crifigvic.hsrplanner.team.dto;

import com.crifigvic.hsrplanner.team.GameMode;
import com.crifigvic.hsrplanner.team.validation.TeamValidationResult;

import java.time.Instant;
import java.util.List;

public record TeamResponse(
        String shareCode,
        String name,
        GameMode gameMode,
        String notes,
        Instant createdAt,
        /** Last edit by its owner, or null. */
        Instant updatedAt,
        /** True when the person asking owns it, so the page can offer Edit and Delete. */
        boolean editable,
        List<TeamSlotResponse> slots,
        TeamValidationResult validation
) {
}
