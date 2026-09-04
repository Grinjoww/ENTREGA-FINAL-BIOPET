package com.biopet.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * Builds RFC 7807 {@link ProblemDetail} instances with BIOPET's error
 * type URNs, used by {@link GlobalExceptionHandler} to keep error
 * responses consistent.
 */
public final class ProblemDetailFactory {

    private ProblemDetailFactory() {
    }

    /**
     * Builds a {@link ProblemDetail} with the given status, BIOPET error
     * type, title and detail, and the current request's path as its
     * instance URI.
     *
     * @param status HTTP status to report
     * @param type BIOPET error type, used to set the problem's {@code type} URN
     * @param title short, human-readable summary of the problem
     * @param detail human-readable explanation specific to this occurrence
     * @param request the current HTTP request, used to populate the problem instance URI
     * @return a populated problem detail ready to be returned as the response body
     */
    public static ProblemDetail build(HttpStatus status, ProblemType type, String title, String detail,
                                       HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(type.uri());
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
