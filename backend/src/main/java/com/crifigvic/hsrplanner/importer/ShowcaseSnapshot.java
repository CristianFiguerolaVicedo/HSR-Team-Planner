package com.crifigvic.hsrplanner.importer;

import java.time.Duration;
import java.util.List;

/**
 * What a player shows in their in-game profile, as read from Enka or Mihomo.
 * Only showcase (and support) characters are visible: never the whole account.
 *
 * @param ttl how long the provider asks us to keep this before asking again
 */
public record ShowcaseSnapshot(String uid, String nickname, Integer level, List<ShowcaseCharacter> characters,
                               Source source, Duration ttl) {

    /** @param eidolon 0-6 (the API calls it "rank") */
    public record ShowcaseCharacter(int avatarId, int eidolon, Integer level) {
    }

    public enum Source {ENKA, MIHOMO}
}
