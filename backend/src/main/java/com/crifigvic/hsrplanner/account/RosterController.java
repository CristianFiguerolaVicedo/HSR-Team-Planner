package com.crifigvic.hsrplanner.account;

import com.crifigvic.hsrplanner.catalog.CharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.TreeMap;

/**
 * "My characters" of the logged-in user. Under /api/me, so SecurityConfig answers 401 without a
 * session, and PUT also needs the CSRF token. Whose roster it is comes only from the session.
 */
@RestController
@RequestMapping("/api/me/roster")
@RequiredArgsConstructor
public class RosterController {

    private final CurrentUserService currentUser;
    private final CharacterRepository characters;

    /** {@code { "1102": 2, "1306": 0 }}: character id → Eidolon, same shape as the browser roster. */
    @GetMapping
    @Transactional(readOnly = true)
    public Map<Integer, Integer> get(OAuth2AuthenticationToken authentication) {
        return new TreeMap<>(currentUser.current(authentication).getRoster());
    }

    /** Replaces the whole roster (the frontend always sends all of it) and returns what was stored. */
    @PutMapping
    @Transactional
    public Map<Integer, Integer> put(OAuth2AuthenticationToken authentication, @RequestBody Map<Integer, Integer> roster) {
        Map<Integer, Integer> clean = RosterRules.sanitize(roster, characters.findExistingIds(roster.keySet()));
        AppUser user = currentUser.current(authentication);
        user.replaceRoster(clean);
        return clean;
    }
}
