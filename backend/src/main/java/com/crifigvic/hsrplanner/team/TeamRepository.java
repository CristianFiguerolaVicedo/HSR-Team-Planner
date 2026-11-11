package com.crifigvic.hsrplanner.team;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = {"slots", "slots.character", "slots.lightCone"})
    Optional<Team> findByShareCode(String shareCode);

    boolean existsByShareCode(String shareCode);
}
