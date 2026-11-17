package com.crifigvic.hsrplanner.account;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * What a saved roster may contain. Pure Java, so it's unit tested without Spring.
 * Bad values are rejected (400): the frontend never sends them, so they mean a bug or a hand-made request.
 * Ids the catalog doesn't know are dropped instead: an old browser roster may hold them.
 */
public final class RosterRules {

    public static final int MAX_ENTRIES = 300;

    private RosterRules() {
    }

    /** @return the roster to store, sorted by id, with only characters the catalog has */
    public static Map<Integer, Integer> sanitize(Map<Integer, Integer> requested, Collection<Integer> knownIds) {
        if (requested == null) {
            throw new IllegalArgumentException("The roster is missing");
        }
        if (requested.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("A roster has at most " + MAX_ENTRIES + " characters");
        }
        Set<Integer> known = Set.copyOf(knownIds);
        Map<Integer, Integer> clean = new TreeMap<>();
        requested.forEach((id, eidolon) -> {
            if (id == null || eidolon == null || eidolon < 0 || eidolon > 6) {
                throw new IllegalArgumentException("Eidolons go from 0 to 6 (character " + id + ": " + eidolon + ")");
            }
            if (known.contains(id)) {
                clean.put(id, eidolon);
            }
        });
        return clean;
    }
}
