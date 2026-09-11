package com.biopet.security;

import com.biopet.service.UserDetailsServiceImpl;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtCookieService jwtCookieService;
    private final AuthenticationAuditService authenticationAuditService;

    /**
     * Creates the filter with the services needed to resolve and validate request tokens.
     *
     * @param jwtService service verifying tokens and reading their claims
     * @param blacklistService blacklist of revoked token identifiers
     * @param userDetailsService service loading user credentials for the security context
     * @param jwtCookieService service reading tokens from request cookies
     * @param authenticationAuditService audit log for revoked token sightings
     */
    public JwtAuthenticationFilter(JwtService jwtService,
                                   TokenBlacklistService blacklistService,
                                   UserDetailsServiceImpl userDetailsService,
                                   JwtCookieService jwtCookieService,
                                   AuthenticationAuditService authenticationAuditService) {
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.userDetailsService = userDetailsService;
        this.jwtCookieService = jwtCookieService;
        this.authenticationAuditService = authenticationAuditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Optional<String> resolvedToken = resolveToken(request);
        if (resolvedToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolvedToken.get();
        try {
            String email = jwtService.extractEmail(token);
            String jti = jwtService.extractJti(token);
            boolean esAccessToken = jwtService.isAccessToken(token);
            boolean revocado = esAccessToken && blacklistService.isRevoked(jti);

            if (revocado) {
                authenticationAuditService.tokenRevoked(request.getRemoteAddr(), email);
            }

            boolean tokenValido = esAccessToken && !revocado;
            if (email != null && tokenValido && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        Optional<String> cookieToken = jwtCookieService.readAccessToken(request);
        if (cookieToken.isPresent()) {
            return cookieToken;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }

        return Optional.empty();
    }
}
