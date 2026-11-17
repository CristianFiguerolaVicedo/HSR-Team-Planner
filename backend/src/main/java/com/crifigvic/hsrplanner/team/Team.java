package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.account.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A shared team. Saved without an account it's an immutable snapshot ("edit a copy" makes a new one);
 * saved while logged in it has an owner, who can edit it in place (same link) or delete it.
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

    /** Last edit by its owner; null if never edited. */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Who saved it while logged in; null for teams saved without an account (those never change). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    public Long ownerId() {
        return owner == null ? null : owner.getId();
    }

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<TeamSlot> slots = new ArrayList<>();

    public void addSlot(TeamSlot slot) {
        slot.setTeam(this);
        slots.add(slot);
    }
}
