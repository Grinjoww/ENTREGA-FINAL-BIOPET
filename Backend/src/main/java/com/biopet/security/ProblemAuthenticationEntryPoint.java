package com.biopet.security;

import com.biopet.exception.ProblemDetailFactory;
import com.biopet.exception.ProblemType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    /**
     * Creates the entry point with the JSON mapper used for problem responses.
     *
     * @param objectMapper mapper serializing problem details to the response body
     */
    public ProblemAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Renders an unauthenticated request as an RFC 7807 unauthorized response.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response receiving the problem body
     * @param authException the authentication failure raised by Spring Security
     * @throws IOException if the problem body cannot be written
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        ProblemDetail problemDetail = ProblemDetailFactory.build(
                HttpStatus.UNAUTHORIZED,
                ProblemType.UNAUTHORIZED,
                "No autenticado",
                "Se requiere una autenticación válida para acceder a este recurso.",
                request
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }
}
