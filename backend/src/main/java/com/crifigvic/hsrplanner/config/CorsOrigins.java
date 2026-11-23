package com.crifigvic.hsrplanner.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the list of origins allowed to call the API.
 *
 * <p>In production the browser only talks to Vercel, which forwards /api to Render. The browser still
 * sends {@code Origin: https://….vercel.app} on POST/PUT/DELETE, while the backend sees its own Render
 * host, so Spring treats those requests as cross-origin and checks them against this list.
 * An origin is scheme + host (+ port) with no path, so a value pasted with a trailing slash
 * ("https://x.vercel.app/") would never match: it's cleaned here, and the site's own address
 * (PUBLIC_URL) is always allowed.</p>
 */
final class CorsOrigins {

    private CorsOrigins() {
    }

    static List<String> of(Collection<String> configured, String publicUrl) {
        Set<String> origins = new LinkedHashSet<>();
        if (configured != null) {
            configured.forEach(origin -> add(origins, origin));
        }
        add(origins, publicUrl);
        return new ArrayList<>(origins);
    }

    private static void add(Set<String> origins, String value) {
        if (value == null) {
            return;
        }
        String origin = value.trim();
        while (origin.endsWith("/")) {
            origin = origin.substring(0, origin.length() - 1);
        }
        if (!origin.isEmpty()) {
            origins.add(origin);
        }
    }
}
