package com.crifigvic.hsrplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterRepository extends JpaRepository<Character, Integer> {

    List<Character> findAllByOrderByNameAsc();
}
