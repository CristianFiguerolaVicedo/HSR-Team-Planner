package com.crifigvic.hsrplanner.importer;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps each UID's snapshot for its ttl, so repeated imports don't hit Enka again
 * (their rules: no repeated or mass requests). Small and in memory: one instance of the API.
 */
public class ShowcaseCache {

    static final int MAX_ENTRIES = 500;

    private record Entry(ShowcaseSnapshot snapshot, Instant expiresAt) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public ShowcaseCache(Clock clock) {
        this.clock = clock;
    }

    public Optional<ShowcaseSnapshot> get(String uid) {
        Entry entry = entries.get(uid);
        if (entry == null) return Optional.empty();
        if (!clock.instant().isBefore(entry.expiresAt())) {
            entries.remove(uid, entry);
            return Optional.empty();
        }
        return Optional.of(entry.snapshot());
    }

    /** @return when the stored snapshot expires */
    public Instant put(ShowcaseSnapshot snapshot) {
        Instant now = clock.instant();
        if (entries.size() >= MAX_ENTRIES) {
            entries.values().removeIf(e -> !now.isBefore(e.expiresAt()));
            if (entries.size() >= MAX_ENTRIES) entries.clear(); // still full: start over rather than grow
        }
        Instant expiresAt = now.plus(snapshot.ttl());
        entries.put(snapshot.uid(), new Entry(snapshot, expiresAt));
        return expiresAt;
    }

    public Optional<Instant> expiresAt(String uid) {
        return Optional.ofNullable(entries.get(uid)).map(Entry::expiresAt);
    }
}
