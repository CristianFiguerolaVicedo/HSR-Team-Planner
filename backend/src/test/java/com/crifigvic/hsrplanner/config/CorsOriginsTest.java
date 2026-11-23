package com.crifigvic.hsrplanner.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorsOriginsTest {

    @Test
    void keepsTheConfiguredOriginsAndAddsThePublicUrl() {
        assertEquals(List.of("http://localhost:5173", "https://hsr-team-planner.vercel.app"),
                CorsOrigins.of(List.of("http://localhost:5173"), "https://hsr-team-planner.vercel.app"));
    }

    @Test
    void removesTrailingSlashesAndSpaces() {
        assertEquals(List.of("https://hsr-team-planner.vercel.app"),
                CorsOrigins.of(List.of(" https://hsr-team-planner.vercel.app/ "), "https://hsr-team-planner.vercel.app//"));
    }

    @Test
    void ignoresAMissingPublicUrlAndEmptyValues() {
        assertEquals(List.of("http://localhost:5173"), CorsOrigins.of(List.of("http://localhost:5173", ""), ""));
        assertEquals(List.of("http://localhost:5173"), CorsOrigins.of(List.of("http://localhost:5173"), null));
        assertEquals(List.of(), CorsOrigins.of(null, null));
    }
}
