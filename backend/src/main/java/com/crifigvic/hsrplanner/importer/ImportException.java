package com.crifigvic.hsrplanner.importer;

import lombok.Getter;

/** Why an import failed, in words a player understands. The handler turns the reason into an HTTP status. */
@Getter
public class ImportException extends RuntimeException {

    public enum Reason {
        /** 400: not a UID. */
        INVALID_UID,
        /** 404: no player with that UID. */
        NOT_FOUND,
        /** 503: game maintenance or update, the APIs can't read profiles. */
        MAINTENANCE,
        /** 429: too many requests to Enka/Mihomo. */
        RATE_LIMITED,
        /** 502: both providers failed or answered something unexpected. */
        UNAVAILABLE
    }

    private final Reason reason;

    public ImportException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ImportException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }
}
