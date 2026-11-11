package com.crifigvic.hsrplanner.team.dto;

import jakarta.validation.constraints.*;

public record TeamSlotRequest(
        @Min(0) @Max(3) int position,
        @NotNull Integer characterId,
        Long lightConeId,
        @Min(0) @Max(6) int eidolon,
        @Min(1) @Max(5) Integer superimposition,
        @DecimalMin("1") @DecimalMax("1000") Double spd
) {
}
