package com.crifigvic.hsrplanner.team;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = {"slots", "slots.character", "slots.lightCone"})
    Optional<Team> findByShareCode(String shareCode);

    boolean existsByShareCode(String shareCode);

    /** "My teams", newest first, with members loaded for the list. */
    @EntityGraph(attributePaths = {"slots", "slots.character"})
    List<Team> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    long countByOwnerId(Long ownerId);

    /** Teams saved without an account. */
    long countByOwnerIsNull();
}
