package com.crifigvic.hsrplanner.auth;

import java.util.Map;

/**
 * The logged-in user, the same shape whatever the provider. Only what the app shows is kept:
 * no email, no tokens.
 *
 * @param provider   "discord" or "google"
 * @param providerId the user's id at that provider (Discord snowflake, Google "sub")
 */
public record AuthenticatedUser(String provider, String providerId, String name, String avatarUrl) {

    /**
     * Builds the user from the attributes each provider returns.
     * <ul>
     *   <li>Discord {@code /users/@me}: {@code id}, {@code username}, {@code global_name} (display name, may be null),
     *       {@code avatar} (an image hash, may be null).</li>
     *   <li>Google OpenID Connect: {@code sub}, {@code name}, {@code picture}.</li>
     * </ul>
     */
    public static AuthenticatedUser from(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "discord" -> {
                String id = text(attributes.get("id"));
                String name = firstNonBlank(text(attributes.get("global_name")), text(attributes.get("username")), "Discord user");
                String avatar = text(attributes.get("avatar"));
                String avatarUrl = id != null && avatar != null
                        ? "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + ".png?size=64"
                        : null;
                yield new AuthenticatedUser(provider, id, name, avatarUrl);
            }
            case "google" -> new AuthenticatedUser(provider, text(attributes.get("sub")),
                    firstNonBlank(text(attributes.get("name")), "Google user", null), text(attributes.get("picture")));
            default -> throw new IllegalArgumentException("Unknown login provider: " + provider);
        };
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return fallback;
    }
}
