package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.account.CurrentUserService;
import com.crifigvic.hsrplanner.team.dto.TeamRequest;
import com.crifigvic.hsrplanner.team.dto.TeamResponse;
import com.crifigvic.hsrplanner.team.validation.TeamValidationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teams;
    private final CurrentUserService currentUser;

    /** Live check while building: roles, synergies and rule issues. Saves nothing. */
    @PostMapping("/validate")
    public TeamValidationResult validate(@Valid @RequestBody TeamRequest request) {
        return teams.validate(request);
    }

    /**
     * Saves the team and returns its share code. 422 if it breaks a rule.
     * Logged in: the team gets an owner, who can edit or delete it later. Anonymous: immutable snapshot.
     */
    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request, Principal principal) {
        TeamResponse created = teams.create(request, currentUser.currentOrNull(principal));
        return ResponseEntity.created(URI.create("/api/teams/" + created.shareCode())).body(created);
    }

    /** Public. "editable" in the answer tells the owner's browser to offer Edit and Delete. */
    @GetMapping("/{shareCode}")
    public TeamResponse get(@PathVariable String shareCode, Principal principal) {
        return teams.get(shareCode, currentUser.currentOrNull(principal));
    }

    /** Owner only (401 without a session, 403 for anyone else). The link doesn't change. */
    @PutMapping("/{shareCode}")
    public TeamResponse update(@PathVariable String shareCode, @Valid @RequestBody TeamRequest request,
                               OAuth2AuthenticationToken authentication) {
        return teams.update(shareCode, request, currentUser.current(authentication));
    }

    /** Owner only. The link stops working. */
    @DeleteMapping("/{shareCode}")
    public ResponseEntity<Void> delete(@PathVariable String shareCode, OAuth2AuthenticationToken authentication) {
        teams.delete(shareCode, currentUser.current(authentication));
        return ResponseEntity.noContent().build();
    }
}
