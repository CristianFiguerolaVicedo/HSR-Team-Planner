package com.crifigvic.hsrplanner.importer;

import com.crifigvic.hsrplanner.importer.ShowcaseSnapshot.ShowcaseCharacter;
import com.crifigvic.hsrplanner.importer.ShowcaseSnapshot.Source;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Answers shaped like Enka's and Mihomo's JSON, as Jackson turns them into maps and lists. */
class ShowcaseParsersTest {

    @Test
    void readsEnkaShowcaseAndSupportCharacters() {
        Map<String, Object> body = Map.of(
                "ttl", 90,
                "uid", "800333171",
                "detailInfo", Map.of(
                        "nickname", "Cristian",
                        "level", 70,
                        "avatarDetailList", List.of(
                                Map.of("avatarId", 1102, "rank", 2, "level", 80),
                                Map.of("avatarId", 1306, "level", 80)),   // no rank = E0
                        "assistAvatarList", List.of(Map.of("avatarId", 1208, "rank", 1, "level", 80))));

        ShowcaseSnapshot snapshot = ShowcaseParsers.enka("800333171", body);

        assertEquals("Cristian", snapshot.nickname());
        assertEquals(Integer.valueOf(70), snapshot.level());
        assertEquals(Source.ENKA, snapshot.source());
        assertEquals(Duration.ofSeconds(90), snapshot.ttl());
        assertEquals(List.of(
                new ShowcaseCharacter(1102, 2, 80),
                new ShowcaseCharacter(1306, 0, 80),
                new ShowcaseCharacter(1208, 1, 80)), snapshot.characters());
    }

    @Test
    void sameCharacterInShowcaseAndSupportCountsOnceWithTheHighestEidolon() {
        Map<String, Object> body = Map.of("detailInfo", Map.of(
                "avatarDetailList", List.of(Map.of("avatarId", 1102, "rank", 1)),
                "assistAvatarList", List.of(Map.of("avatarId", 1102, "rank", 3))));

        assertEquals(List.of(new ShowcaseCharacter(1102, 3, null)), ShowcaseParsers.enka("800333171", body).characters());
    }

    @Test
    void enkaWithAnEmptyOrMissingShowcaseGivesNoCharacters() {
        assertTrue(ShowcaseParsers.enka("800333171", Map.of("detailInfo", Map.of("nickname", "x"))).characters().isEmpty());
        assertTrue(ShowcaseParsers.enka("800333171", Map.of()).characters().isEmpty());
    }

    @Test
    void ttlStaysBetweenOneAndTenMinutes() {
        assertEquals(Duration.ofSeconds(60), ShowcaseParsers.enka("1", Map.of("ttl", 5, "detailInfo", Map.of())).ttl());
        assertEquals(Duration.ofMinutes(10), ShowcaseParsers.enka("1", Map.of("ttl", 99_999, "detailInfo", Map.of())).ttl());
        assertEquals(Duration.ofSeconds(60), ShowcaseParsers.enka("1", Map.of("detailInfo", Map.of())).ttl());
    }

    @Test
    void eidolonsOutOfRangeAreClamped() {
        Map<String, Object> body = Map.of("detailInfo", Map.of("avatarDetailList", List.of(
                Map.of("avatarId", 1, "rank", 9), Map.of("avatarId", 2, "rank", -1))));
        List<ShowcaseCharacter> characters = ShowcaseParsers.enka("1", body).characters();
        assertEquals(6, characters.get(0).eidolon());
        assertEquals(0, characters.get(1).eidolon());
    }

    @Test
    void entriesWithoutAnIdAreSkipped() {
        Map<String, Object> body = Map.of("detailInfo", Map.of("avatarDetailList", List.of(
                Map.of("rank", 2), "not an object", Map.of("avatarId", 1102))));
        assertEquals(List.of(new ShowcaseCharacter(1102, 0, null)), ShowcaseParsers.enka("1", body).characters());
    }

    @Test
    void readsMihomoWithIdsAsText() {
        Map<String, Object> body = Map.of(
                "player", Map.of("uid", "800333171", "nickname", "Cristian", "level", 70),
                "characters", List.of(
                        Map.of("id", "1102", "name", "Seele", "rank", 2, "level", 80),
                        Map.of("id", 8006L, "name", "Trailblazer", "rank", 6, "level", 80)));

        ShowcaseSnapshot snapshot = ShowcaseParsers.mihomo("800333171", body);

        assertEquals(Source.MIHOMO, snapshot.source());
        assertEquals("Cristian", snapshot.nickname());
        assertEquals(List.of(new ShowcaseCharacter(1102, 2, 80), new ShowcaseCharacter(8006, 6, 80)), snapshot.characters());
    }

    @Test
    void integerAcceptsNumbersAndDigitsOnly() {
        assertEquals(Integer.valueOf(1102), ShowcaseParsers.integer(1102));
        assertEquals(Integer.valueOf(1102), ShowcaseParsers.integer(1102L));
        assertEquals(Integer.valueOf(1102), ShowcaseParsers.integer("1102"));
        assertNull(ShowcaseParsers.integer("11a2"));
        assertNull(ShowcaseParsers.integer(null));
    }
}
