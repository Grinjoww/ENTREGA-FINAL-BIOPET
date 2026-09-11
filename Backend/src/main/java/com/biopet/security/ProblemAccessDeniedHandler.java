package com.biopet.security;

import com.biopet.exception.ProblemDetailFactory;
import com.biopet.exception.ProblemType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    /**
     * Creates the handler with the JSON mapper used for problem responses.
     *
     * @param objectMapper mapper serializing problem details to the response body
     */
    public ProblemAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Renders an authenticated but unauthorized request as an RFC 7807 forbidden response.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response receiving the problem body
     * @param accessDeniedException the authorization failure raised by Spring Security
     * @throws IOException if the problem body cannot be written
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        ProblemDetail problemDetail = ProblemDetailFactory.build(
                HttpStatus.FORBIDDEN,
                ProblemType.FORBIDDEN,
                "Acceso denegado",
                "No tiene permisos suficientes para acceder a este recurso.",
                request
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }
}
