package com.biopet.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuthenticationAuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationAuditService.class);

    private static final String UNKNOWN = "unknown";
    private static final int MAX_LENGTH = 200;

    private static final String EVENT_LOGIN_SUCCEEDED = "LOGIN_SUCCESS";
    private static final String EVENT_LOGIN_FAILED = "LOGIN_FAILURE";
    private static final String EVENT_LOGIN_BLOCKED = "LOGIN_RATE_LIMITED";
    private static final String EVENT_REFRESH_SUCCEEDED = "REFRESH_SUCCESS";
    private static final String EVENT_REFRESH_FAILED = "REFRESH_FAILURE";
    private static final String EVENT_LOGOUT_SUCCEEDED = "LOGOUT_SUCCESS";
    private static final String EVENT_TOKEN_REVOKED = "TOKEN_REVOKED";

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAILURE = "FAILURE";
    private static final String RESULT_BLOCKED = "BLOCKED";

    /**
     * Records a successful login.
     *
     * @param ip client address of the request, or {@code unknown} when unavailable
     * @param subject authenticated user email, or {@code unknown} when unavailable
     */
    public void loginSucceeded(String ip, String subject) {
        logger.info(format(EVENT_LOGIN_SUCCEEDED, RESULT_SUCCESS, ip, subject));
    }

    /**
     * Records a failed login attempt.
     *
     * @param ip client address of the request, or {@code unknown} when unavailable
     * @param subject attempted user email, or {@code unknown} when unavailable
     */
    public void loginFailed(String ip, String subject) {
        logger.warn(format(EVENT_LOGIN_FAILED, RESULT_FAILURE, ip, subject));
    }

    /**
     * Records a login rejected by rate limiting.
     *
     * @param ip client address of the request, or {@code unknown} when unavailable
     * @param subject attempted user email, or {@code unknown} when unavailable
     */
    public void loginBlocked(String ip, String subject) {
        logger.warn(format(EVENT_LOGIN_BLOCKED, RESULT_BLOCKED, ip, subject));
    }

    /**
     * Records a successful token refresh.
     *
     * @param ip client address of the request, or {@code unknown} when unavailable
     * @param subject authenticated user email, or {@code unknown} when unavailable
     */
    public void refreshSucceeded(String ip, String subject) {
        logger.info(format(EVENT_REFRESH_SUCCEEDED, RESULT_SUCCESS, ip, subject));
    }

    /**
     * Records a failed token refresh.
     *
     * @param ip client address of the request, or {@code unknown} when unavailable
     * @param subject attempted user email, or {@code unknown} when unavailable
     */
    public void refreshFailed(String ip, String subject) {
        logger.warn(format(EVENT_REFRESH_FAILED, RESULT_FAILURE, ip, subject));
    }

    /**
     * Records a successful logout.
     *
     * @param ip client address of the request, or {@code unknown} when unavailable
     * @param subject authenticated user email, or {@code unknown} when unavailable
     */
    public void logoutSucceeded(String ip, String subject) {
        logger.info(format(EVENT_LOGOUT_SUCCEEDED, RESULT_SUCCESS, ip, subject));
    }

    /**
     * Records the revocation of a refresh token.
     *
     * @param ip client address of the request, or {@code unknown} when unavailable
     * @param subject owner email of the revoked token, or {@code unknown} when unavailable
     */
    public void tokenRevoked(String ip, String subject) {
        logger.warn(format(EVENT_TOKEN_REVOKED, RESULT_BLOCKED, ip, subject));
    }

    private String format(String event, String result, String ip, String subject) {
        return "AUTH_AUDIT timestamp=" + Instant.now()
                + " event=" + event
                + " result=" + result
                + " ip=" + normalize(ip)
                + " subject=" + normalize(subject);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String sanitized = value.replaceAll("\\p{Cntrl}", "");
        if (sanitized.isBlank()) {
            return UNKNOWN;
        }
        return sanitized.length() > MAX_LENGTH
                ? sanitized.substring(0, MAX_LENGTH)
                : sanitized;
    }
}
