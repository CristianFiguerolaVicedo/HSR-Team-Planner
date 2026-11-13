package com.crifigvic.hsrplanner.sync;

import com.crifigvic.hsrplanner.catalog.*;
import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.AscensionJson;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.CharacterJson;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.LightConeJson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copies characters and light cones from HSRWiki into our database and rebuilds the
 * BUILD_GUIDE synergies. Safe to run any number of times (upsert):
 * rows are never deleted, so saved teams keep pointing at valid characters.
 */
@Slf4j
@Service
public class CatalogSyncService {

    private final HsrWikiClient hsrWiki;
    private final CharacterRepository characters;
    private final LightConeRepository lightCones;
    private final SynergyRepository synergies;
    private final CatalogOverrides overrides;
    private final Clock clock;
    private final SynergyExtractor synergyExtractor = new SynergyExtractor();

    public CatalogSyncService(HsrWikiClient hsrWiki, CharacterRepository characters, LightConeRepository lightCones,
                              SynergyRepository synergies, ObjectMapper objectMapper, Clock clock) {
        this.hsrWiki = hsrWiki;
        this.characters = characters;
        this.lightCones = lightCones;
        this.synergies = synergies;
        this.overrides = CatalogOverrides.load(objectMapper);
        this.clock = clock;
    }

    public boolean catalogIsEmpty() {
        return characters.count() == 0;
    }

    /**
     * Downloads first, then writes in one transaction: if HSRWiki is down nothing changes,
     * and readers never see a half-updated catalog.
     */
    @Transactional
    public SyncReport sync() {
        Instant startedAt = Instant.now(clock);
        log.info("Catalog sync started");

        List<CharacterJson> characterJson = hsrWiki.characters();
        List<LightConeJson> lightConeJson = hsrWiki.lightCones();
        String gameVersion = hsrWiki.gameVersion();

        List<String> skipped = new ArrayList<>();
        CharacterNames names = new CharacterNames();
        Map<Integer, Character> savedCharacters = upsertCharacters(characterJson, names, skipped, startedAt);
        int savedLightCones = upsertLightCones(lightConeJson, skipped, startedAt);
        int savedSynergies = rebuildSynergies(characterJson, names, savedCharacters);

        SyncReport report = new SyncReport(gameVersion, savedCharacters.size(), savedLightCones, savedSynergies,
                List.copyOf(skipped), startedAt, Instant.now(clock));
        log.info("Catalog sync finished: {}", report);
        return report;
    }

    private Map<Integer, Character> upsertCharacters(List<CharacterJson> json, CharacterNames names,
                                                     List<String> skipped, Instant now) {
        Map<Integer, Character> existing = characters.findAll().stream()
                .collect(Collectors.toMap(Character::getId, Function.identity()));

        // Register every name first so overrides and aliases can be resolved
        json.forEach(c -> names.register(c.name(), c.id()));
        overrides.aliases().forEach((alias, target) -> {
            if (!names.addAlias(alias, target)) {
                log.warn("Alias '{}' points to unknown character '{}'", alias, target);
            }
        });
        Map<Integer, List<Role>> roleOverrides = resolveOverrides(overrides.roles(), names);
        Map<Integer, List<Tag>> tagOverrides = resolveOverrides(overrides.tags(), names);
        Map<Integer, String> groupOverrides = resolveOverrides(overrides.variantGroups(), names);

        Map<Integer, Character> saved = new LinkedHashMap<>();
        for (CharacterJson c : json) {
            Element element = parse(Element.class, c.element());
            Path path = parse(Path.class, c.path());
            if (element == null || path == null) {
                skipped.add("character " + c.name() + " (element " + c.element() + ", path " + c.path() + ")");
                continue;
            }
            Character entity = existing.getOrDefault(c.id(), new Character());
            entity.setId(c.id());
            entity.setName(CharacterNames.displayName(c.name()));
            entity.setRarity(c.rarity());
            entity.setElement(element);
            entity.setPath(path);
            List<Role> roles = roleOverrides.getOrDefault(c.id(), List.of(Role.defaultFor(path)));
            // Change the managed list in place: Hibernate tracks that collection
            entity.getRoles().clear();
            entity.getRoles().addAll(roles.stream().distinct().toList());
            entity.getTags().clear();
            entity.getTags().addAll(tagOverrides.getOrDefault(c.id(), List.of()));
            entity.setBaseSpd(baseSpd(c.ascension()));
            entity.setVariantGroup(groupOverrides.getOrDefault(c.id(), CharacterNames.builtInVariantGroup(c.name())));
            entity.setIconUrl(c.icon());
            entity.setMiniIconUrl(c.miniIcon());
            entity.setSyncedAt(now);
            saved.put(c.id(), entity);
        }
        characters.saveAll(saved.values());
        return saved;
    }

    private int upsertLightCones(List<LightConeJson> json, List<String> skipped, Instant now) {
        Map<String, LightCone> existing = lightCones.findAll().stream()
                .collect(Collectors.toMap(LightCone::getName, Function.identity()));
        List<LightCone> toSave = new ArrayList<>();
        for (LightConeJson lc : json) {
            Path path = parse(Path.class, lc.path());
            if (lc.name() == null || path == null) {
                skipped.add("light cone " + lc.name() + " (path " + lc.path() + ")");
                continue;
            }
            LightCone entity = existing.getOrDefault(lc.name(), new LightCone());
            entity.setName(lc.name());
            entity.setHsrwikiId(lc.id());
            entity.setRarity(Objects.requireNonNullElse(lc.rarity(), 3));
            entity.setPath(path);
            entity.setIconUrl(lc.icon());
            entity.setSyncedAt(now);
            toSave.add(entity);
        }
        lightCones.saveAll(toSave);
        return toSave.size();
    }

    private int rebuildSynergies(List<CharacterJson> json, CharacterNames names, Map<Integer, Character> saved) {
        synergies.deleteAllBySource(SynergySource.BUILD_GUIDE);
        List<Synergy> toSave = new ArrayList<>();
        synergyExtractor.extract(json, names).forEach((pair, score) -> {
            Character a = saved.get(pair.lowId());
            Character b = saved.get(pair.highId());
            if (a != null && b != null) {
                toSave.add(Synergy.builder()
                        .characterA(a)
                        .characterB(b)
                        .score(score.value())
                        .source(SynergySource.BUILD_GUIDE)
                        .reason(score.reason())
                        .build());
            }
        });
        synergies.saveAll(toSave);
        return toSave.size();
    }

    /** Highest SPD across ascension phases (SPD doesn't grow with level, but this is safe either way). */
    static double baseSpd(List<AscensionJson> ascension) {
        if (ascension == null) {
            return 0;
        }
        return ascension.stream()
                .filter(a -> a != null && a.spd() != null && a.spd().base() != null)
                .mapToDouble(a -> a.spd().base())
                .max()
                .orElse(0);
    }

    private static <T> Map<Integer, T> resolveOverrides(Map<String, T> byName, CharacterNames names) {
        Map<Integer, T> byId = new HashMap<>();
        byName.forEach((name, value) -> {
            Set<Integer> ids = names.resolve(name);
            if (ids.isEmpty()) {
                log.warn("Override for unknown character '{}'", name);
            }
            ids.forEach(id -> byId.put(id, value));
        });
        return byId;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
