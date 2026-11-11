package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.team.dto.TeamRequest;
import com.crifigvic.hsrplanner.team.dto.TeamResponse;
import com.crifigvic.hsrplanner.team.validation.TeamValidationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teams;

    /** Live check while building: roles, synergies and rule issues. Saves nothing. */
    @PostMapping("/validate")
    public TeamValidationResult validate(@Valid @RequestBody TeamRequest request) {
        return teams.validate(request);
    }

    /** Saves the team and returns its share code. 422 if it breaks a rule. */
    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        TeamResponse created = teams.create(request);
        return ResponseEntity.created(URI.create("/api/teams/" + created.shareCode())).body(created);
    }

    @GetMapping("/{shareCode}")
    public TeamResponse get(@PathVariable String shareCode) {
        return teams.get(shareCode);
    }
}
