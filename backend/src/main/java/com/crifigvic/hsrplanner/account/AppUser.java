package com.crifigvic.hsrplanner.account;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Someone who logged in. Named AppUser so it never gets mixed up with Spring Security's User.
 * Created (or refreshed) by {@link CurrentUserService} with an upsert, never from user input.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "avatar_url", length = 300)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    /** "My characters": character id → Eidolon. One row per character in user_roster. */
    @ElementCollection
    @CollectionTable(name = "user_roster", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "character_id")
    @Column(name = "eidolon", nullable = false)
    private Map<Integer, Integer> roster = new HashMap<>();

    /** Replaces the whole roster. Changing the managed map in place lets Hibernate track it. */
    public void replaceRoster(Map<Integer, Integer> newRoster) {
        roster.keySet().retainAll(newRoster.keySet());
        roster.putAll(newRoster);
    }
}
