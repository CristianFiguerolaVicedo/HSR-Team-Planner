package com.crifigvic.hsrplanner.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginFailureHandlerTest {

    @Test
    void addsTheReasonToTheFailureUrl() {
        assertEquals("https://x.vercel.app/?login=error&reason=authorization_request_not_found",
                LoginFailureHandler.redirectUrl("https://x.vercel.app/?login=error", "authorization_request_not_found"));
        assertEquals("/oops?reason=invalid_token_response",
                LoginFailureHandler.redirectUrl("/oops", "invalid_token_response"));
    }

    @Test
    void keepsOnlySafeCharactersInTheReason() {
        assertEquals("/?login=error&reason=scriptalert1script",
                LoginFailureHandler.redirectUrl("/?login=error", "<script>alert(1)</script>"));
    }

    @Test
    void leavesTheUrlAloneWithoutAReason() {
        assertEquals("/?login=error", LoginFailureHandler.redirectUrl("/?login=error", null));
        assertEquals("/?login=error", LoginFailureHandler.redirectUrl("/?login=error", "<>"));
    }
}
