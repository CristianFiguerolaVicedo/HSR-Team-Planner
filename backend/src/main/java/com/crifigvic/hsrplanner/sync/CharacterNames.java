package com.crifigvic.hsrplanner.sync;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the names found in HSRWiki into character ids.
 *
 * <ul>
 *   <li>HSRWiki names the Trailblazer {@code TrailblazerHarmony#F}; we show "Trailblazer (Harmony) · F"
 *       and "Trailblazer (Harmony)" points to both the M and F versions.</li>
 *   <li>Matching ignores case, spaces and punctuation: "Dan Heng • Imbibitor Lunae" = "dan heng imbibitor lunae".</li>
 *   <li>Short names used by the build guides ("Topaz") come from the aliases in {@code catalog/overrides.json}.</li>
 *   <li>Placeholders like "Any Sustain" match nothing and are skipped.</li>
 * </ul>
 */
public class CharacterNames {

    private static final Pattern TRAILBLAZER = Pattern.compile("^Trailblazer(\\p{Alpha}+)#([MF])$");
    public static final String TRAILBLAZER_GROUP = "TRAILBLAZER";

    private final Map<String, Set<Integer>> idsByKey = new HashMap<>();
    private final Map<Integer, String> variantGroupById = new HashMap<>();

    /** Name we store and show for an HSRWiki character name. */
    public static String displayName(String hsrwikiName) {
        Matcher m = TRAILBLAZER.matcher(hsrwikiName);
        return m.matches() ? "Trailblazer (" + m.group(1) + ") · " + m.group(2) : hsrwikiName;
    }

    /** Built-in variant group, or null. More groups can be added in overrides.json. */
    public static String builtInVariantGroup(String hsrwikiName) {
        return TRAILBLAZER.matcher(hsrwikiName).matches() ? TRAILBLAZER_GROUP : null;
    }

    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public void register(String hsrwikiName, int id) {
        add(displayName(hsrwikiName), id);
        String group = builtInVariantGroup(hsrwikiName);
        if (group != null) {
            variantGroupById.put(id, group);
        }
        Matcher m = TRAILBLAZER.matcher(hsrwikiName);
        if (m.matches()) {
            add("Trailblazer (" + m.group(1) + ")", id);
        }
    }

    /** {@code alias} resolves to whatever {@code target} resolves to. Unknown targets are ignored. */
    public boolean addAlias(String alias, String target) {
        Set<Integer> ids = idsByKey.get(normalize(target));
        if (ids == null) {
            return false;
        }
        ids.forEach(id -> add(alias, id));
        return true;
    }

    public Set<Integer> resolve(String name) {
        return idsByKey.getOrDefault(normalize(name), Set.of());
    }

    /** Whether two ids are versions of the same character (Trailblazer M and F, for instance). */
    public boolean sameCharacter(int id, int otherId) {
        String group = variantGroupById.get(id);
        return id == otherId || (group != null && group.equals(variantGroupById.get(otherId)));
    }

    private void add(String name, int id) {
        idsByKey.computeIfAbsent(normalize(name), k -> new LinkedHashSet<>()).add(id);
    }
}
