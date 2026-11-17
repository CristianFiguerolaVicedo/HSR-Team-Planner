package com.crifigvic.hsrplanner.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Creates the user, or refreshes name and avatar if it already exists, in one statement.
     * PostgreSQL's ON CONFLICT makes it safe when two requests of a brand-new user arrive at once
     * (a "find, then insert" would let both insert and one would fail on the unique constraint).
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO users (provider, provider_id, name, avatar_url, created_at, last_seen_at)
            VALUES (:provider, :providerId, :name, :avatarUrl, :now, :now)
            ON CONFLICT (provider, provider_id)
            DO UPDATE SET name = EXCLUDED.name, avatar_url = EXCLUDED.avatar_url, last_seen_at = EXCLUDED.last_seen_at
            """)
    void upsert(String provider, String providerId, String name, String avatarUrl, Instant now);
}
