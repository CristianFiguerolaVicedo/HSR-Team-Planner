package com.crifigvic.hsrplanner.sync;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** HSRWiki kept failing after the retries. Answered as 503 by POST /api/admin/sync. */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class HsrWikiUnavailableException extends RuntimeException {

    public HsrWikiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
