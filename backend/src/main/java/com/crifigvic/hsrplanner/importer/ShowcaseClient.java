package com.crifigvic.hsrplanner.importer;

import com.crifigvic.hsrplanner.config.PlannerProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Objects;

/** Raw HTTP calls to Enka.Network and Mihomo. Answers come back as plain JSON trees for ShowcaseParsers. */
@Component
public class ShowcaseClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {};
    /** Enka asks every app to identify itself, so they can contact the author if something goes wrong. */
    static final String USER_AGENT = "HSR-Team-Planner (+https://github.com/CristianFiguerolaVicedo/HSR-Team-Planner)";

    private final RestClient enka;
    private final RestClient mihomo;

    public ShowcaseClient(PlannerProperties properties) {
        PlannerProperties.Importer config = properties.importer();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(config.timeout());

        this.enka = RestClient.builder().baseUrl(config.enkaUrl()).requestFactory(requestFactory)
                .defaultHeader("User-Agent", USER_AGENT).build();
        this.mihomo = RestClient.builder().baseUrl(config.mihomoUrl()).requestFactory(requestFactory)
                .defaultHeader("User-Agent", USER_AGENT).build();
    }

    /** {@code GET {enka}/api/hsr/uid/{uid}} */
    public Map<String, Object> enka(String uid) {
        return Objects.requireNonNullElse(enka.get().uri("/api/hsr/uid/{uid}", uid).retrieve().body(JSON_OBJECT), Map.of());
    }

    /** {@code GET {mihomo}/sr_info_parsed/{uid}?lang=en} */
    public Map<String, Object> mihomo(String uid) {
        return Objects.requireNonNullElse(
                mihomo.get().uri("/sr_info_parsed/{uid}?lang=en", uid).retrieve().body(JSON_OBJECT), Map.of());
    }
}
