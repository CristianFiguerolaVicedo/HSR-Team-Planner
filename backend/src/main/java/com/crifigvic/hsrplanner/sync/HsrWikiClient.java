package com.crifigvic.hsrplanner.sync;

import com.crifigvic.hsrplanner.config.PlannerProperties;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.CharacterJson;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.LightConeJson;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reads the public HSRWiki API (github.com/CristianFiguerolaVicedo/HSRWiki-backend). */
@Component
public class HsrWikiClient {

    private final RestClient http;

    public HsrWikiClient(PlannerProperties properties) {
        PlannerProperties.HsrWiki config = properties.hsrwiki();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(config.timeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(config.timeout());

        this.http = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "HSR-Team-Planner (+https://github.com/CristianFiguerolaVicedo/HSR-Team-Planner)")
                .build();
    }

    public List<CharacterJson> characters() {
        return Objects.requireNonNullElse(
                http.get().uri("/api/characters").retrieve().body(new ParameterizedTypeReference<List<CharacterJson>>() {}),
                List.of());
    }

    public List<LightConeJson> lightCones() {
        return Objects.requireNonNullElse(
                http.get().uri("/api/lightcones").retrieve().body(new ParameterizedTypeReference<List<LightConeJson>>() {}),
                List.of());
    }

    /** Game version of HSRWiki's data, e.g. "4.6.0". */
    public String gameVersion() {
        Map<?, ?> body = http.get().uri("/api/version").retrieve().body(Map.class);
        return body == null ? null : Objects.toString(body.get("gameVersion"), null);
    }
}
