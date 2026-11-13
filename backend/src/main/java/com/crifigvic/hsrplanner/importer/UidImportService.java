package com.crifigvic.hsrplanner.importer;

import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.catalog.CharacterRepository;
import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;
import com.crifigvic.hsrplanner.importer.ImportException.Reason;
import com.crifigvic.hsrplanner.importer.ShowcaseSnapshot.ShowcaseCharacter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads a player's in-game showcase by UID: Enka.Network first, Mihomo if Enka is down.
 * Results are cached for the time Enka asks, as its usage rules require.
 */
@Slf4j
@Service
public class UidImportService {

    /** HSR UIDs: 9 digits (10 on some servers), never starting with 0. */
    static final Pattern UID = Pattern.compile("[1-9]\\d{8,9}");

    private final ShowcaseClient client;
    private final CharacterRepository characters;
    private final TransactionTemplate readOnly;
    private final ShowcaseCache cache;

    public UidImportService(ShowcaseClient client, CharacterRepository characters,
                            PlatformTransactionManager transactions, Clock clock) {
        this.client = client;
        this.characters = characters;
        this.readOnly = new TransactionTemplate(transactions);
        this.readOnly.setReadOnly(true);
        this.cache = new ShowcaseCache(clock);
    }

    public ImportResult importUid(String rawUid) {
        String uid = rawUid == null ? "" : rawUid.strip();
        if (!UID.matcher(uid).matches()) {
            throw new ImportException(Reason.INVALID_UID, "A UID has 9 or 10 digits, like 800333171");
        }
        ShowcaseSnapshot snapshot = cache.get(uid).orElseGet(() -> fetch(uid));
        Instant refreshAfter = cache.expiresAt(uid).orElse(null);
        // Roles and tags are lazy collections: build the DTOs inside a read transaction
        return readOnly.execute(status -> toResult(snapshot, refreshAfter));
    }

    // --- providers -----------------------------------------------------------------------------

    private ShowcaseSnapshot fetch(String uid) {
        ImportException enkaFailure;
        try {
            Map<String, Object> body = client.enka(uid);
            if (body.get("detailInfo") instanceof Map<?, ?>) {
                ShowcaseSnapshot snapshot = ShowcaseParsers.enka(uid, body);
                cache.put(snapshot);
                return snapshot;
            }
            enkaFailure = new ImportException(Reason.UNAVAILABLE, "Enka answered without player data");
        } catch (RestClientResponseException e) {
            enkaFailure = fromStatus(e.getStatusCode().value(), "Enka", e);
            // These answers would be the same on Mihomo: same game servers behind both
            if (enkaFailure.getReason() == Reason.INVALID_UID || enkaFailure.getReason() == Reason.NOT_FOUND
                    || enkaFailure.getReason() == Reason.MAINTENANCE) {
                throw enkaFailure;
            }
        } catch (RestClientException e) {
            enkaFailure = new ImportException(Reason.UNAVAILABLE, "Enka didn't answer", e);
        }

        log.warn("Enka import failed for a UID ({}), trying Mihomo", enkaFailure.getMessage());
        try {
            Map<String, Object> body = client.mihomo(uid);
            if (!(body.get("player") instanceof Map<?, ?>)) {
                throw enkaFailure;
            }
            ShowcaseSnapshot snapshot = ShowcaseParsers.mihomo(uid, body);
            cache.put(snapshot);
            return snapshot;
        } catch (RestClientResponseException e) {
            ImportException mihomoFailure = fromStatus(e.getStatusCode().value(), "Mihomo", e);
            throw mihomoFailure.getReason() == Reason.NOT_FOUND ? mihomoFailure : enkaFailure;
        } catch (RestClientException e) {
            throw enkaFailure;
        }
    }

    /** Status codes documented by Enka; Mihomo uses the usual ones. */
    static ImportException fromStatus(int status, String provider, Throwable cause) {
        return switch (status) {
            case 400 -> new ImportException(Reason.INVALID_UID, "That UID isn't valid", cause);
            case 404 -> new ImportException(Reason.NOT_FOUND, "No player with that UID", cause);
            case 424 -> new ImportException(Reason.MAINTENANCE,
                    "The game is under maintenance or just updated: profiles can't be read for now", cause);
            case 429 -> new ImportException(Reason.RATE_LIMITED, "Too many requests: try again in a minute", cause);
            default -> new ImportException(Reason.UNAVAILABLE, provider + " isn't available right now (" + status + ")", cause);
        };
    }

    // --- mapping -------------------------------------------------------------------------------

    private ImportResult toResult(ShowcaseSnapshot snapshot, Instant refreshAfter) {
        Set<Integer> ids = snapshot.characters().stream().map(ShowcaseCharacter::avatarId).collect(Collectors.toSet());
        Map<Integer, Character> found = characters.findAllById(ids).stream()
                .collect(Collectors.toMap(Character::getId, Function.identity()));

        List<ImportResult.ImportedCharacter> imported = new ArrayList<>();
        List<Integer> unknown = new ArrayList<>();
        for (ShowcaseCharacter c : snapshot.characters()) {
            Character character = found.get(c.avatarId());
            if (character == null) {
                unknown.add(c.avatarId());
            } else {
                imported.add(new ImportResult.ImportedCharacter(CharacterDto.from(character), c.eidolon(), c.level()));
            }
        }
        return new ImportResult(snapshot.uid(), snapshot.nickname(), snapshot.level(), snapshot.source(),
                imported, unknown, refreshAfter);
    }
}
