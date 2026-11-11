package com.crifigvic.hsrplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LightConeRepository extends JpaRepository<LightCone, Long> {

    Optional<LightCone> findByName(String name);

    List<LightCone> findAllByOrderByRarityDescNameAsc();
}
