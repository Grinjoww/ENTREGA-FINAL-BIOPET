package com.biopet.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central mapping of exceptions raised anywhere in the request pipeline
 * to RFC 7807 {@link ProblemDetail} responses, so callers get a
 * consistent JSON error shape instead of a raw stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maps a duplicate-email registration attempt to HTTP 409.
     *
     * @param ex the raised exception, carrying the offending email in its message
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a 409 Conflict problem response
     */
    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ProblemDetail> emailDuplicado(EmailDuplicadoException ex, HttpServletRequest request) {
        return problemResponse(HttpStatus.CONFLICT, ProblemType.CONFLICT, "Conflicto de datos", ex.getMessage(), request);
    }

    /**
     * Maps a missing-resource lookup to HTTP 404.
     *
     * @param ex the raised exception, carrying a description of the missing resource
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a 404 Not Found problem response
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return problemResponse(HttpStatus.NOT_FOUND, ProblemType.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), request);
    }

    /**
     * Maps a failed login attempt (wrong email/password) to HTTP 401,
     * without echoing back which part of the credentials was invalid.
     *
     * @param ex the raised exception (message intentionally not exposed to the client)
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a 401 Unauthorized problem response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> credencialesInvalidas(BadCredentialsException ex, HttpServletRequest request) {
        return problemResponse(HttpStatus.UNAUTHORIZED, ProblemType.UNAUTHORIZED, "No autenticado", "Credenciales inválidas", request);
    }

    /**
     * Maps Bean Validation failures on request bodies to HTTP 422,
     * including the per-field validation messages.
     *
     * @param ex the raised exception, carrying the field-level validation errors
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a 422 Unprocessable Entity problem response with an {@code errors} property listing invalid fields
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, List<String>> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.computeIfAbsent(fieldError.getField(), key -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }

        ProblemDetail problemDetail = ProblemDetailFactory.build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ProblemType.VALIDATION,
                "Error de validación",
                "Uno o más campos contienen valores inválidos.",
                request
        );
        problemDetail.setProperty("errors", errores);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    /**
     * Maps a domain-level invalid-argument condition (e.g. a referenced
     * user not having the expected role) to HTTP 400.
     *
     * @param ex the raised exception, carrying a description of the invalid argument
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a 400 Bad Request problem response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> argumentoInvalido(IllegalArgumentException ex, HttpServletRequest request) {
        return problemResponse(HttpStatus.BAD_REQUEST, ProblemType.BAD_REQUEST, "Solicitud inválida", ex.getMessage(), request);
    }

    /**
     * Maps a request/path parameter that Spring could not convert to
     * its target type (e.g. a non-numeric id) to HTTP 400.
     *
     * @param ex the raised exception, carrying the offending parameter name
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a 400 Bad Request problem response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detail = "El parámetro '" + ex.getName() + "' tiene un formato inválido.";
        return problemResponse(HttpStatus.BAD_REQUEST, ProblemType.BAD_REQUEST, "Parámetro inválido", detail, request);
    }

    /**
     * Maps an exceeded login rate limit to HTTP 429, including a
     * {@code Retry-After} header with the remaining lockout time.
     *
     * @param ex the raised exception, carrying the remaining lockout time in seconds
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a 429 Too Many Requests problem response with a {@code Retry-After} header
     */
    @ExceptionHandler(RateLimitExcedidoException.class)
    public ResponseEntity<ProblemDetail> demasiadosIntentos(RateLimitExcedidoException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.build(
                HttpStatus.TOO_MANY_REQUESTS,
                ProblemType.RATE_LIMITED,
                "Demasiados intentos",
                "Se ha excedido el número máximo de intentos fallidos de inicio de sesión. Intente nuevamente más tarde.",
                request
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosRestantes()))
                .body(problemDetail);
    }

    private ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, ProblemType type, String title,
                                                            String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.build(status, type, title, detail, request);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }
    /**
     * Maps a failure to reach or parse the external species-information
     * API to HTTP 502, without exposing the upstream error detail to
     * the client.
     *
     * @param ex the raised exception (upstream cause available via {@link Throwable#getCause()})
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a 502 Bad Gateway problem response
     */
    @ExceptionHandler(ExternalApiException.class)
public ResponseEntity<ProblemDetail> errorApiExterna(ExternalApiException ex, HttpServletRequest request) {
    return problemResponse(HttpStatus.BAD_GATEWAY, ProblemType.BAD_GATEWAY,
            "Servicio externo no disponible",
            "No se pudo obtener información de la especie en este momento. Intente nuevamente más tarde.",
            request);
}
}
