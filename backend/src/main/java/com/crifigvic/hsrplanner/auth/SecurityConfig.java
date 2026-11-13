package com.crifigvic.hsrplanner.auth;

import com.crifigvic.hsrplanner.config.PlannerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * Who can call what, and how login works.
 *
 * <ul>
 *   <li>Everything that already existed stays public: the app works without an account.</li>
 *   <li>{@code /api/me/**} needs a session; without one the answer is 401 (not a redirect to a login page).</li>
 *   <li>Login: the browser opens {@code /oauth2/authorization/discord} (or google); Spring Security does the
 *       OAuth2 authorization-code flow and keeps the user in the server session (JSESSIONID, HttpOnly cookie).</li>
 *   <li>CSRF: because the session lives in a cookie, every POST/PUT/DELETE must also carry the token from
 *       the XSRF-TOKEN cookie in the X-XSRF-TOKEN header. axios does that by itself on same-origin requests.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, PlannerProperties properties) throws Exception {
        PlannerProperties.Auth auth = properties.auth();

        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/me/**").authenticated()
                        .anyRequest().permitAll())

                .oauth2Login(login -> login
                        .defaultSuccessUrl(auth.successUrl(), true)
                        .failureUrl(auth.failureUrl()))

                // An API answers 401; it never redirects a fetch() to an HTML login page
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies("JSESSIONID"))

                .csrf(csrf -> csrf
                        // Cookie readable by JavaScript (that's the point: the SPA copies it into a header)
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        // Admin sync is called with curl + X-Admin-Token, not from the browser
                        .ignoringRequestMatchers("/api/admin/**"))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        return http.build();
    }
}
