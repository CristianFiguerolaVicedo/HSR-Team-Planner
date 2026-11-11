package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.catalog.LightCone;
import jakarta.persistence.*;
import lombok.*;

/** One of the (up to) 4 places in a team, with the build details that matter for planning. */
@Entity
@Table(name = "team_slots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "position"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    /** 0 to 3, left to right in the game's team screen. */
    @Column(nullable = false)
    private int position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id")
    private Character character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "light_cone_id")
    private LightCone lightCone;

    /** E0 to E6. */
    @Column(nullable = false)
    private int eidolon;

    /** S1 to S5, null without a light cone. */
    private Integer superimposition;

    /** Final in-combat SPD of this build, if the user filled it in. Drives the SPD analysis. */
    private Double spd;
}
