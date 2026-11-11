package com.crifigvic.hsrplanner.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A light cone, copied from HSRWiki by the sync.
 * HSRWiki builds light cone ids from {@code name.hashCode()}, so we match on the name
 * and keep our own primary key: links to saved teams survive if HSRWiki changes its ids.
 */
@Entity
@Table(name = "light_cones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LightCone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hsrwiki_id")
    private Integer hsrwikiId;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false)
    private int rarity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Path path;

    @Column(name = "icon_url", length = 300)
    private String iconUrl;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;
}
