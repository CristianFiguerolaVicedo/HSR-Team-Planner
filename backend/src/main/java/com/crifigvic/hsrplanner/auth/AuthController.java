package com.crifigvic.hsrplanner.auth;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {

    /** Placeholder used in application.yml when a provider has no credentials. */
    static final String NOT_CONFIGURED = "not-configured";
    private static final List<String> PROVIDERS = List.of("discord", "google");

    private final ClientRegistrationRepository registrations;

    public AuthController(ClientRegistrationRepository registrations) {
        this.registrations = registrations;
    }

    public record LoginProvider(String id, String name, String loginUrl) {
    }

    /** Login buttons to show: only providers with real credentials. Public. */
    @GetMapping("/auth/providers")
    public List<LoginProvider> providers() {
        List<LoginProvider> configured = new ArrayList<>();
        for (String id : PROVIDERS) {
            ClientRegistration registration = registrations.findByRegistrationId(id);
            if (registration != null && !NOT_CONFIGURED.equals(registration.getClientId())) {
                // Spring Security's default login path; the browser navigates there (not fetch)
                configured.add(new LoginProvider(id, registration.getClientName(), "/oauth2/authorization/" + id));
            }
        }
        return configured;
    }

    /**
     * Who is logged in. Under /api/me, so without a session Spring answers 401 before reaching here.
     * The token is the one Spring created after the OAuth login; its attributes come from the provider.
     */
    @GetMapping("/me")
    public AuthenticatedUser me(OAuth2AuthenticationToken authentication) {
        return AuthenticatedUser.from(authentication.getAuthorizedClientRegistrationId(),
                authentication.getPrincipal().getAttributes());
    }
}
