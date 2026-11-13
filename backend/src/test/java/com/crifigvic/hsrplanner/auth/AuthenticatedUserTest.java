package com.crifigvic.hsrplanner.auth;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticatedUserTest {

    @Test
    void discordUsesTheDisplayNameAndBuildsTheAvatarUrl() {
        AuthenticatedUser user = AuthenticatedUser.from("discord", Map.of(
                "id", "80351110224678912", "username", "nelly", "global_name", "Nelly", "avatar", "8342729096ea3675442027381ff50dfe"));

        assertEquals("discord", user.provider());
        assertEquals("80351110224678912", user.providerId());
        assertEquals("Nelly", user.name());
        assertEquals("https://cdn.discordapp.com/avatars/80351110224678912/8342729096ea3675442027381ff50dfe.png?size=64", user.avatarUrl());
    }

    @Test
    void discordWithoutDisplayNameOrAvatarFallsBack() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "1");
        attributes.put("username", "nelly");
        attributes.put("global_name", null);
        attributes.put("avatar", null);

        AuthenticatedUser user = AuthenticatedUser.from("discord", attributes);

        assertEquals("nelly", user.name());
        assertNull(user.avatarUrl());
    }

    @Test
    void googleUsesSubNameAndPicture() {
        AuthenticatedUser user = AuthenticatedUser.from("google", Map.of(
                "sub", "1098", "name", "Cristian", "picture", "https://lh3.googleusercontent.com/a/x", "email", "not@kept.com"));

        assertEquals("1098", user.providerId());
        assertEquals("Cristian", user.name());
        assertEquals("https://lh3.googleusercontent.com/a/x", user.avatarUrl());
    }

    @Test
    void neverKeepsTheEmail() {
        AuthenticatedUser user = AuthenticatedUser.from("google", Map.of("sub", "1", "email", "not@kept.com"));
        assertFalse(user.toString().contains("not@kept.com"));
    }

    @Test
    void unknownProvidersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedUser.from("github", Map.of("id", "1")));
    }
}
