package com.crifigvic.hsrplanner.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A playable character, copied from HSRWiki by the sync. The id is the game's own id (e.g. 1005). */
@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

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
