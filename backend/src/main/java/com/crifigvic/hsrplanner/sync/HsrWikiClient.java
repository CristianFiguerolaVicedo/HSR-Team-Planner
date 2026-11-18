package com.crifigvic.hsrplanner.sync;

import com.crifigvic.hsrplanner.config.PlannerProperties;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.CharacterJson;
import com.crifigvic.hsrplanner.sync.HsrWikiDtos.LightConeJson;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Reads the public HSRWiki API (github.com/CristianFiguerolaVicedo/HSRWiki-backend). */
@Component
public class HsrWikiClient {

    // HSRWiki runs on Render's free plan too: while it wakes up, Render answers 502/503 or the
    // connection times out. Keep trying for a few minutes before calling the sync a failure.
    private static final Duration GIVE_UP_AFTER = Duration.ofMinutes(4);
    private static final Duration WAIT = Duration.ofSeconds(10);

    private final RestClient http;
    private final Retrier retrier = Retrier.waitingFor(GIVE_UP_AFTER, WAIT);

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
        return Objects.requireNonNullElse(withRetries("/api/characters", () ->
                http.get().uri("/api/characters").retrieve().body(new ParameterizedTypeReference<List<CharacterJson>>() {})),
                List.of());
    }

    public List<LightConeJson> lightCones() {
        return Objects.requireNonNullElse(withRetries("/api/lightcones", () ->
                http.get().uri("/api/lightcones").retrieve().body(new ParameterizedTypeReference<List<LightConeJson>>() {})),
                List.of());
    }

    /** Game version of HSRWiki's data, e.g. "4.6.0". */
    public String gameVersion() {
        Map<?, ?> body = withRetries("/api/version", () -> http.get().uri("/api/version").retrieve().body(Map.class));
        return body == null ? null : Objects.toString(body.get("gameVersion"), null);
    }

    private <T> T withRetries(String path, Supplier<T> call) {
        try {
            return retrier.call(call, HsrWikiClient::isWakingUp);
        } catch (RestClientResponseException e) {
            // Short message: the exception's own text includes the whole HTML error page
            throw new HsrWikiUnavailableException(
                    "HSRWiki " + path + " answered " + e.getStatusCode().value() + " " + e.getStatusText(), e);
        } catch (ResourceAccessException e) {
            throw new HsrWikiUnavailableException("HSRWiki " + path + " did not answer: " + e.getMessage(), e);
        }
    }

    /** What a sleeping or starting server answers: no connection, a timeout, or 502/503/504. */
    static boolean isWakingUp(RuntimeException e) {
        if (e instanceof ResourceAccessException) {
            return true;
        }
        if (e instanceof HttpServerErrorException server) {
            int status = server.getStatusCode().value();
            return status == 502 || status == 503 || status == 504;
        }
        return false;
    }
}
