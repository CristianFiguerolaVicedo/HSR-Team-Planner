package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.account.CurrentUserService;
import com.crifigvic.hsrplanner.team.dto.TeamSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** "My teams": under /api/me, so it needs a session; only the caller's own teams. */
@RestController
@RequiredArgsConstructor
public class MyTeamsController {

    private final TeamService teams;
    private final CurrentUserService currentUser;

    @GetMapping("/api/me/teams")
    public List<TeamSummary> myTeams(OAuth2AuthenticationToken authentication) {
        return teams.listMine(currentUser.current(authentication));
    }
}
