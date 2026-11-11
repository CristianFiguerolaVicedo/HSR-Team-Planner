package com.crifigvic.hsrplanner.sync;

import com.crifigvic.hsrplanner.catalog.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;

/**
 * Contents of {@code catalog/overrides.json}.
 *
 * @param aliases       short name used by the build guides → real character name
 * @param roles         role that wins over {@link Role#defaultFor}
 * @param variantGroups extra "can't be in the same team" groups, character name → group
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogOverrides(Map<String, String> aliases, Map<String, Role> roles, Map<String, String> variantGroups) {

    public static final String RESOURCE = "catalog/overrides.json";

    public CatalogOverrides {
        aliases = Objects.requireNonNullElse(aliases, Map.of());
        roles = Objects.requireNonNullElse(roles, Map.of());
        variantGroups = Objects.requireNonNullElse(variantGroups, Map.of());
    }

    public static CatalogOverrides load(ObjectMapper mapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            return mapper.readValue(in, CatalogOverrides.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + RESOURCE, e);
        }
    }
}
