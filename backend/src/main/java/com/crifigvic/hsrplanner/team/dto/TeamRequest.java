package com.crifigvic.hsrplanner.team.dto;

import com.crifigvic.hsrplanner.team.GameMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TeamRequest(
        @NotBlank @Size(max = 60) String name,
        GameMode gameMode,
        @Size(max = 500) String notes,
        @NotEmpty @Size(max = 4) List<@Valid TeamSlotRequest> slots
) {
}
