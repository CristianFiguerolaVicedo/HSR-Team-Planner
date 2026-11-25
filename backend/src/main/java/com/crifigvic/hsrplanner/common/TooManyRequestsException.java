package com.crifigvic.hsrplanner.common;

/** Answered as 429: a rate limit was reached (see RateLimitFilter and TeamController). */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
