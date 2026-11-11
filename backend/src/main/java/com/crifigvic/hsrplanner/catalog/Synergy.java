package com.crifigvic.hsrplanner.catalog;

import jakarta.persistence.*;
import lombok.*;

/**
 * How well two characters work together. Stored once per pair with
 * {@code characterA.id < characterB.id}, so (A, B) and (B, A) can't both exist.
 */
@Entity
@Table(name = "synergies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"character_a_id", "character_b_id", "source"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Synergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_a_id")
    private Character characterA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_b_id")
    private Character characterB;

    /** Higher is better. BUILD_GUIDE: +2 per recommended team as a fixed member, +1 as an alternative. */
    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SynergySource source;

    /** Why: the recommended team names it came from, or a hand-written note. */
    @Column(length = 500)
    private String reason;
}
