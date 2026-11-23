package com.crifigvic.hsrplanner.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * What happens when Discord/Google login fails. Spring's default just redirects and logs nothing,
 * so a failure in production was impossible to diagnose. This one logs the reason and passes its
 * OAuth2 error code (e.g. authorization_request_not_found, invalid_token_response) to the site,
 * which shows it in the error banner. Only the code goes in the URL, never the message.
 */
class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginFailureHandler.class);

    private final String failureUrl;

    LoginFailureHandler(String failureUrl) {
        this.failureUrl = failureUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String code = exception instanceof OAuth2AuthenticationException oauth
                ? oauth.getError().getErrorCode()
                : exception.getClass().getSimpleName();
        log.warn("Login failed ({}): {}", code, exception.getMessage());
        response.sendRedirect(redirectUrl(failureUrl, code));
    }

    /** failureUrl plus reason=<code>, with the code reduced to letters, digits and _ so nothing odd reaches the page. */
    static String redirectUrl(String failureUrl, String code) {
        String reason = code == null ? "" : code.replaceAll("[^A-Za-z0-9_]", "");
        if (reason.isEmpty()) {
            return failureUrl;
        }
        return failureUrl + (failureUrl.contains("?") ? "&" : "?") + "reason=" + reason;
    }
}
