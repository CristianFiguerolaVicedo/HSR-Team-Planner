package com.crifigvic.hsrplanner.importer;

import com.crifigvic.hsrplanner.importer.ShowcaseSnapshot.ShowcaseCharacter;
import com.crifigvic.hsrplanner.importer.ShowcaseSnapshot.Source;

import java.time.Duration;
import java.util.*;

/**
 * Reads Enka and Mihomo answers. Works on the plain Map/List tree Jackson gives for JSON, so the
 * rules can be tested without HTTP. Tolerant on purpose: both APIs are unofficial and change with
 * game updates, so missing fields get defaults instead of failing the whole import.
 */
public final class ShowcaseParsers {

    /** Enka asks to wait at least this long; used when the answer has no ttl. */
    static final Duration DEFAULT_TTL = Duration.ofSeconds(60);
    static final Duration MAX_TTL = Duration.ofMinutes(10);

    private ShowcaseParsers() {
    }

    /**
     * Enka: {@code GET /api/hsr/uid/{uid}} →
     * {@code { ttl, uid, detailInfo: { nickname, level, avatarDetailList: [{ avatarId, rank, level }], assistAvatarList: [...] } }}.
     * The game leaves out fields equal to 0, so a missing {@code rank} means E0.
     */
    public static ShowcaseSnapshot enka(String uid, Map<String, Object> body) {
        Map<String, Object> detail = map(body.get("detailInfo"));
        List<ShowcaseCharacter> characters = new ArrayList<>();
        for (String list : List.of("avatarDetailList", "assistAvatarList")) {
            for (Object item : list(detail.get(list))) {
                Map<String, Object> avatar = map(item);
                Integer id = integer(avatar.get("avatarId"));
                if (id != null) {
                    characters.add(new ShowcaseCharacter(id, eidolon(avatar.get("rank")), integer(avatar.get("level"))));
                }
            }
        }
        Integer ttlSeconds = integer(body.get("ttl"));
        Duration ttl = ttlSeconds == null ? DEFAULT_TTL : Duration.ofSeconds(ttlSeconds);
        return new ShowcaseSnapshot(uid, text(detail.get("nickname")), integer(detail.get("level")),
                dedupe(characters), Source.ENKA, clampTtl(ttl));
    }

    /**
     * Mihomo: {@code GET /sr_info_parsed/{uid}?lang=en} →
     * {@code { player: { uid, nickname, level }, characters: [{ id: "1102", rank, level }] }}.
     */
    public static ShowcaseSnapshot mihomo(String uid, Map<String, Object> body) {
        Map<String, Object> player = map(body.get("player"));
        List<ShowcaseCharacter> characters = new ArrayList<>();
        for (Object item : list(body.get("characters"))) {
            Map<String, Object> avatar = map(item);
            Integer id = integer(avatar.get("id"));
            if (id != null) {
                characters.add(new ShowcaseCharacter(id, eidolon(avatar.get("rank")), integer(avatar.get("level"))));
            }
        }
        return new ShowcaseSnapshot(uid, text(player.get("nickname")), integer(player.get("level")),
                dedupe(characters), Source.MIHOMO, DEFAULT_TTL);
    }

    /** The same character can be in the showcase and as support: keep the highest Eidolon once. */
    private static List<ShowcaseCharacter> dedupe(List<ShowcaseCharacter> characters) {
        Map<Integer, ShowcaseCharacter> byId = new LinkedHashMap<>();
        for (ShowcaseCharacter c : characters) {
            byId.merge(c.avatarId(), c, (a, b) -> b.eidolon() > a.eidolon() ? b : a);
        }
        return List.copyOf(byId.values());
    }

    private static Duration clampTtl(Duration ttl) {
        if (ttl.compareTo(DEFAULT_TTL) < 0) return DEFAULT_TTL;
        return ttl.compareTo(MAX_TTL) > 0 ? MAX_TTL : ttl;
    }

    private static int eidolon(Object value) {
        Integer rank = integer(value);
        return rank == null ? 0 : Math.clamp(rank, 0, 6);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> l ? l : List.of();
    }

    /** Numbers may come as Integer, Long or even text ("1102" in Mihomo). */
    static Integer integer(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s && s.matches("\\d{1,9}")) return Integer.valueOf(s);
        return null;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
