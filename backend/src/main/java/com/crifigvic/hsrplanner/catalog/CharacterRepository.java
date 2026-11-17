package com.crifigvic.hsrplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;

import java.util.List;

public interface CharacterRepository extends JpaRepository<Character, Integer> {

    List<Character> findAllByOrderByNameAsc();

    /** Which of these ids exist, without loading the characters. */
    @Query("select c.id from Character c where c.id in :ids")
    Set<Integer> findExistingIds(Collection<Integer> ids);
}
