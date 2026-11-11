package com.crifigvic.hsrplanner.spd;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spd")
public class SpdController {

    private final SpdCalculator calculator = new SpdCalculator();

    /** {@code GET /api/spd?speed=134.2&cycles=3} → analysis of one unit. */
    @GetMapping
    public SpdAnalysis analyze(@RequestParam double speed,
                               @RequestParam(defaultValue = "3") int cycles) {
        return calculator.analyze(speed, cycles);
    }

    /** {@code POST /api/spd/team} → analysis of up to 4 units at once, in the same order. */
    @PostMapping("/team")
    public List<SpdAnalysis> analyzeTeam(@Valid @RequestBody TeamSpdRequest request) {
        return request.speeds().stream()
                .map(speed -> calculator.analyze(speed, request.cycles()))
                .toList();
    }

    public record TeamSpdRequest(
            @NotEmpty @Size(max = 4) List<Double> speeds,
            @Min(1) @Max(SpdCalculator.MAX_CYCLES) int cycles) {
    }
}
