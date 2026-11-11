package com.crifigvic.hsrplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface SynergyRepository extends JpaRepository<Synergy, Long> {

    @Query("""
            select s from Synergy s
            join fetch s.characterA join fetch s.characterB
            where s.characterA.id = :characterId or s.characterB.id = :characterId
            order by s.score desc
            """)
    List<Synergy> findAllInvolving(Integer characterId);

    /** Every synergy where both characters are in {@code characterIds}: the pairs inside one team. */
    @Query("""
            select s from Synergy s
            join fetch s.characterA join fetch s.characterB
            where s.characterA.id in :characterIds and s.characterB.id in :characterIds
            """)
    List<Synergy> findAllWithin(Collection<Integer> characterIds);

    @Modifying
    @Query("delete from Synergy s where s.source = :source")
    int deleteAllBySource(SynergySource source);
}
