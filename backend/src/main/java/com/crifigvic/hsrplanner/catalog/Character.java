package com.crifigvic.hsrplanner.catalog;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** A playable character, copied from HSRWiki by the sync. The id is the game's own id (e.g. 1005). */
@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
public class Character {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false)
    private int rarity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Element element;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Path path;

    /**
     * Roles in order, the first being the main one. SUBSELECT loads the roles of every character
     * in the list with one extra query instead of one per character.
     */
    @ElementCollection
    @CollectionTable(name = "character_roles", joinColumns = @JoinColumn(name = "character_id"))
    @OrderColumn(name = "role_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Fetch(FetchMode.SUBSELECT)
    private List<Role> roles = new ArrayList<>();

    /** Damage profile (AoE, single target, break); empty for most supports. */
    @ElementCollection
    @CollectionTable(name = "character_tags", joinColumns = @JoinColumn(name = "character_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 20)
    @Fetch(FetchMode.SUBSELECT)
    private Set<Tag> tags = EnumSet.noneOf(Tag.class);

    /** Main role: the first one. */
    public Role mainRole() {
        return roles.isEmpty() ? Role.defaultFor(path) : roles.getFirst();
    }

    /** Base SPD at max ascension, before light cone, relics and traces. */
    @Column(name = "base_spd", nullable = false)
    private double baseSpd;

    /**
     * Characters sharing a group can't be in the same team (every Trailblazer path is the same person).
     * Null for everyone else.
     */
    @Column(name = "variant_group", length = 40)
    private String variantGroup;

    @Column(name = "icon_url", length = 300)
    private String iconUrl;

    @Column(name = "mini_icon_url", length = 300)
    private String miniIconUrl;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;
}
