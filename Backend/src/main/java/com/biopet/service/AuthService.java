package com.biopet.service;

import com.biopet.entity.Rol;
import com.biopet.dto.*;
import com.biopet.entity.Usuario;
import com.biopet.exception.EmailDuplicadoException;
import com.biopet.exception.RateLimitExcedidoException;
import com.biopet.exception.RecursoNoEncontradoException;
import com.biopet.repository.UsuarioRepository;
import com.biopet.security.AuthenticationAuditService;
import com.biopet.security.JwtService;
import com.biopet.security.LoginRateLimiterService;
import com.biopet.security.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service for authentication: registration, login, token
 * refresh, logout and reading the authenticated user's own profile.
 * Coordinates {@link JwtService} (token issuance/parsing),
 * {@link TokenBlacklistService} (revocation), {@link LoginRateLimiterService}
 * (per-IP failed-attempt rate limiting) and
 * {@link AuthenticationAuditService} (audit logging) around the persisted
 * {@link Usuario}.
 */
@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final LoginRateLimiterService loginRateLimiterService;
    private final AuthenticationAuditService authenticationAuditService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       TokenBlacklistService blacklistService,
                       LoginRateLimiterService loginRateLimiterService,
                       AuthenticationAuditService authenticationAuditService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.loginRateLimiterService = loginRateLimiterService;
        this.authenticationAuditService = authenticationAuditService;
    }

    /**
     * Registers a new user with role {@code ROLE_DUENO}, hashing the
     * given password with {@link PasswordEncoder} and normalizing the
     * email to lower case before persisting it.
     *
     * @param request registration data (name, email, password)
     * @return the newly created user
     * @throws EmailDuplicadoException if a user with that email already
     *         exists
     */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new EmailDuplicadoException(request.email());
        }
        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .rol(Rol.ROLE_DUENO)
                .activo(true)
                .build();
        Usuario guardado = usuarioRepository.save(usuario);
        return toResponse(guardado);
    }

    /**
     * Authenticates a user by email and password, enforcing a per-IP
     * failed-attempt rate limit via {@link LoginRateLimiterService} and
     * recording the outcome (blocked, failed, or successful) via
     * {@link AuthenticationAuditService}.
     *
     * @param request login credentials (email, password)
     * @param ip caller's IP address, used for rate limiting and audit
     * @return a new access token, a new refresh token, and the access
     *         token's expiration in seconds
     * @throws RateLimitExcedidoException if the per-IP failed-attempt
     *         limit was already exceeded, before or after this attempt
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the email/password pair is invalid
     * @throws RecursoNoEncontradoException if authentication succeeds but
     *         no active user exists for that email
     */
    public AuthResponse login(LoginRequest request, String ip) {
        String emailSolicitado = request.email().toLowerCase();

        try {
            loginRateLimiterService.verificarPermitido(ip);
        } catch (RateLimitExcedidoException ex) {
            authenticationAuditService.loginBloqueado(ip, emailSolicitado);
            throw ex;
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailSolicitado, request.password())
            );
        } catch (BadCredentialsException ex) {
            try {
                loginRateLimiterService.registrarFallo(ip);
            } catch (RateLimitExcedidoException limiteExcedido) {
                authenticationAuditService.loginBloqueado(ip, emailSolicitado);
                throw limiteExcedido;
            }
            authenticationAuditService.loginFallido(ip, emailSolicitado);
            throw ex;
        }

        loginRateLimiterService.reiniciar(ip);
        authenticationAuditService.loginExitoso(ip, authentication.getName());

        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no existe"));
        return new AuthResponse(
                jwtService.generateAccessToken(usuario),
                jwtService.generateRefreshToken(usuario),
                jwtService.getExpirationMs() / 1000
        );
    }

    /**
     * Issues a new access token from a valid, non-revoked refresh token.
     * The given refresh token is returned unchanged in the response; this
     * method does not rotate it.
     *
     * @param request the refresh token to validate
     * @param ip caller's IP address, used for audit
     * @return a new access token, the same refresh token, and the access
     *         token's expiration in seconds
     * @throws IllegalArgumentException if the refresh token is absent,
     *         blank, not a refresh token, or has already been revoked
     * @throws io.jsonwebtoken.JwtException if the token is malformed,
     *         expired, or fails signature/issuer/audience verification
     *         (propagated from {@link JwtService#extractClaims})
     * @throws RecursoNoEncontradoException if the token is otherwise
     *         valid but no active user exists for its subject
     */
    public AuthResponse refresh(RefreshRequest request, String ip) {
        String refreshToken = request.refreshToken();
        String emailVerificado = null;
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalArgumentException("Refresh token ausente");
            }
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new IllegalArgumentException("El token enviado no es un refresh token");
            }
            emailVerificado = jwtService.extractEmail(refreshToken);
            String jti = jwtService.extractJti(refreshToken);
            if (blacklistService.isRevoked(jti)) {
                throw new IllegalArgumentException("Refresh token revocado");
            }
            Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(emailVerificado)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

            authenticationAuditService.refreshExitoso(ip, emailVerificado);
            return new AuthResponse(jwtService.generateAccessToken(usuario), refreshToken, jwtService.getExpirationMs() / 1000);
        } catch (RuntimeException ex) {
            authenticationAuditService.refreshFallido(ip, emailVerificado);
            throw ex;
        }
    }

    /**
     * Revokes the given access and refresh tokens (via
     * {@link #revocarYObtenerSubject}) and records the outcome via
     * {@link AuthenticationAuditService}. A token that is {@code null},
     * empty, or otherwise invalid/expired is silently skipped rather than
     * rejected; this method never throws for that reason.
     *
     * @param accessToken access token to revoke, or {@code null}
     * @param refreshToken refresh token to revoke, or {@code null}
     * @param ip caller's IP address, used for audit
     */
    public void logout(String accessToken, String refreshToken, String ip) {
        String subjectAccess = revocarYObtenerSubject(accessToken);
        String subjectRefresh = revocarYObtenerSubject(refreshToken);
        String subject = (subjectAccess != null) ? subjectAccess : subjectRefresh;
        authenticationAuditService.logoutExitoso(ip, subject);
    }

    private String revocarYObtenerSubject(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            String jti = jwtService.extractJti(token);
            Instant expiresAt = jwtService.extractExpiration(token);
            blacklistService.revoke(jti, expiresAt);
            return jwtService.extractEmail(token);
        } catch (JwtException | IllegalArgumentException ex) {
            // Token inválido, expirado o no procesable: no se revoca, no se propaga el error.
            return null;
        }
    }

    /**
     * Looks up the profile of an active user by email.
     *
     * @param email the user's email
     * @return the user's profile
     * @throws RecursoNoEncontradoException if no active user exists with
     *         that email
     */
    public UsuarioResponse perfil(String email) {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return toResponse(usuario);
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol(), usuario.isActivo());
    }
}
