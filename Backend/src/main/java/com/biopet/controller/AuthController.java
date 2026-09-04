package com.biopet.controller;

import com.biopet.dto.*;
import com.biopet.security.JwtCookieService;
import com.biopet.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for authentication: registration, login, refresh and
 * logout. Access and refresh tokens are never returned in the response
 * body; they are set as {@code HttpOnly} cookies via
 * {@link JwtCookieService}, and only their expiration (in seconds) is
 * exposed to the client. Business logic (credential validation, rate
 * limiting, token issuance and revocation) lives in {@link AuthService};
 * this controller only wires HTTP requests/responses to it.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtCookieService jwtCookieService;

    public AuthController(AuthService authService, JwtCookieService jwtCookieService) {
        this.authService = authService;
        this.jwtCookieService = jwtCookieService;
    }

    /**
     * Registers a new user account with role {@code ROLE_DUENO}.
     *
     * @param request registration data (name, email, password), already
     *                 validated by {@code @Valid}
     * @return the created user, with HTTP 201 (Created)
     * @throws com.biopet.exception.EmailDuplicadoException if a user with
     *         that email already exists
     */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /**
     * Authenticates a user and starts a session by setting the access and
     * refresh token cookies on the response.
     *
     * @param request login credentials (email, password), already
     *                validated by {@code @Valid}
     * @param servletRequest incoming request, used to read the caller's
     *                        remote address for rate limiting and audit
     * @param response outgoing response, where the access and refresh
     *                 cookies are set
     * @return HTTP 200 (OK) with the access token's expiration in seconds
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the email/password pair is invalid
     * @throws com.biopet.exception.RateLimitExcedidoException if too many
     *         failed attempts were already made from this IP
     */
    @PostMapping("/login")
    public ResponseEntity<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request,
                                                       HttpServletRequest servletRequest,
                                                       HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request, servletRequest.getRemoteAddr());
        jwtCookieService.addAccessCookie(response, authResponse.accessToken());
        jwtCookieService.addRefreshCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(new AuthSessionResponse(authResponse.expiresIn()));
    }

    /**
     * Issues a new access token cookie from the refresh token carried in
     * the request's cookies (not read from the request body). A missing
     * or invalid refresh token is reported as a generic authentication
     * failure, without distinguishing the exact cause to the client.
     *
     * @param servletRequest incoming request; the refresh token is read
     *                        from its cookie via {@link JwtCookieService}
     * @param servletResponse outgoing response, where the new access
     *                         cookie is set
     * @return HTTP 200 (OK) with the new access token's expiration in
     *         seconds
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the refresh token cookie is absent, malformed, or
     *         otherwise rejected by {@link AuthService#refresh}
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthSessionResponse> refresh(HttpServletRequest servletRequest,
                                                         HttpServletResponse servletResponse) {
        String refreshToken = jwtCookieService.readRefreshToken(servletRequest).orElse(null);

        AuthResponse authResponse;
        try {
            authResponse = authService.refresh(new RefreshRequest(refreshToken), servletRequest.getRemoteAddr());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Refresh token ausente o inválido");
        }

        jwtCookieService.addAccessCookie(servletResponse, authResponse.accessToken());
        return ResponseEntity.ok(new AuthSessionResponse(authResponse.expiresIn()));
    }

    /**
     * Ends the current session: revokes the access and refresh tokens
     * carried in the request's cookies (silently ignoring tokens that are
     * absent, already expired, or otherwise invalid) and clears both
     * cookies on the response regardless of whether a token was present.
     *
     * @param servletRequest incoming request, used to read the access and
     *                        refresh token cookies
     * @param servletResponse outgoing response, where both cookies are
     *                         cleared
     * @return HTTP 204 (No Content)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest,
                                        HttpServletResponse servletResponse) {
        String accessToken = jwtCookieService.readAccessToken(servletRequest).orElse(null);
        String refreshToken = jwtCookieService.readRefreshToken(servletRequest).orElse(null);
        authService.logout(accessToken, refreshToken, servletRequest.getRemoteAddr());

        jwtCookieService.clearAccessCookie(servletResponse);
        jwtCookieService.clearRefreshCookie(servletResponse);

        return ResponseEntity.noContent().build();
    }
}
