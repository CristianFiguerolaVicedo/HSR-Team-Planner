package com.crifigvic.hsrplanner.team;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A shared team. There's no login in the MVP, so a team is an immutable snapshot:
 * "editing" a team in the frontend creates a new one with a new share code.
 */
@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short public id used in links: /t/{shareCode}. */
    @Column(name = "share_code", nullable = false, unique = true, length = ShareCodeGenerator.LENGTH)
    private String shareCode;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", length = 30)
    private GameMode gameMode;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<TeamSlot> slots = new ArrayList<>();

    public void addSlot(TeamSlot slot) {
        slot.setTeam(this);
        slots.add(slot);
    }
}
