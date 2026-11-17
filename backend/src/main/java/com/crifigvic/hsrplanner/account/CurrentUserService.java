package com.crifigvic.hsrplanner.account;

import com.crifigvic.hsrplanner.auth.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;

/**
 * Turns the OAuth login kept in the session into our own database user.
 * The identity always comes from the session (what Discord/Google told us), never from the
 * request body: a user can only ever read or change their own data.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository users;
    private final Clock clock;

    /** Creates or refreshes the user (name, avatar, last seen). Called once per page load, from GET /api/me. */
    @Transactional
    public AppUser touch(OAuth2AuthenticationToken authentication) {
        AuthenticatedUser info = identity(authentication);
        users.upsert(info.provider(), info.providerId(), truncate(info.name(), 100), truncate(info.avatarUrl(), 300),
                Instant.now(clock));
        return users.findByProviderAndProviderId(info.provider(), info.providerId()).orElseThrow();
    }

    /** The current user; created on the spot if GET /api/me hasn't run yet. */
    @Transactional
    public AppUser current(OAuth2AuthenticationToken authentication) {
        AuthenticatedUser info = identity(authentication);
        return users.findByProviderAndProviderId(info.provider(), info.providerId())
                .orElseGet(() -> touch(authentication));
    }

    /** The user if this request comes from someone logged in, null otherwise (public endpoints). */
    @Transactional
    public AppUser currentOrNull(Principal principal) {
        return principal instanceof OAuth2AuthenticationToken token ? current(token) : null;
    }

    /**
     * Deletes the logged-in user. The database does the rest in the same statement:
     * user_roster and teams (with their slots) are ON DELETE CASCADE.
     */
    @Transactional
    public void deleteAccount(OAuth2AuthenticationToken authentication) {
        AuthenticatedUser info = identity(authentication);
        users.findByProviderAndProviderId(info.provider(), info.providerId()).ifPresent(users::delete);
    }

    static AuthenticatedUser identity(OAuth2AuthenticationToken authentication) {
        return AuthenticatedUser.from(authentication.getAuthorizedClientRegistrationId(),
                authentication.getPrincipal().getAttributes());
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
