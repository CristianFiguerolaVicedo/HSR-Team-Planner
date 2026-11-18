package com.crifigvic.hsrplanner.sync;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Another sync is already running (e.g. the startup one). Answered as 409 by POST /api/admin/sync. */
@ResponseStatus(HttpStatus.CONFLICT)
public class SyncInProgressException extends RuntimeException {

    public SyncInProgressException() {
        super("A catalog sync is already running");
    }
}
