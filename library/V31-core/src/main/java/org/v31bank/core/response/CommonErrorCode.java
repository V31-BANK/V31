package org.v31bank.core.response;

/**
 * The failures any service can report, independent of what it does.
 * <p>
 * Failures specific to a domain — an insufficient balance, a frozen wallet, a
 * sanctions hit — belong in that service's own {@link ErrorCode} enum rather than
 * here.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum CommonErrorCode implements ErrorCode {

    /**
     * The request was malformed: an unparseable body, a missing parameter, or a
     * field that failed validation.
     */
    VALIDATION_FAILED(400, "The request is not valid"),

    /**
     * No credentials were presented, or the ones presented were not accepted.
     */
    UNAUTHENTICATED(401, "Authentication is required"),

    /**
     * The caller is known but is not allowed to perform this operation.
     */
    FORBIDDEN(403, "This operation is not permitted"),

    /**
     * The addressed resource does not exist, or is not visible to this caller.
     */
    NOT_FOUND(404, "The requested resource does not exist"),

    /**
     * The request conflicts with the current state, such as a duplicate unique
     * value or an edit against a stale version.
     */
    CONFLICT(409, "The request conflicts with the current state of the resource"),

    /**
     * An {@code Idempotency-Key} was replayed with a different payload, so the
     * original response cannot be returned and the new request must not be
     * applied.
     */
    IDEMPOTENCY_KEY_REUSED(409, "This idempotency key was already used with a different request"),

    /**
     * The request was well formed but a business rule refused it.
     */
    UNPROCESSABLE(422, "The request could not be processed"),

    /**
     * The caller exceeded its quota and should retry later.
     */
    RATE_LIMITED(429, "Too many requests, please retry later"),

    /**
     * The service failed for a reason the caller cannot act on. The message stays
     * deliberately vague; the cause belongs in the logs, correlated by the
     * response's trace identifier.
     */
    INTERNAL_ERROR(500, "The request could not be completed"),

    /**
     * A service or provider this call depends on is unavailable. Distinct from
     * {@link #INTERNAL_ERROR} because retrying is usually worthwhile.
     */
    DEPENDENCY_UNAVAILABLE(503, "A required service is temporarily unavailable"),

    /**
     * A dependency did not answer in time. The outcome of the operation is
     * unknown, so a caller retrying it must send the same
     * {@code Idempotency-Key}.
     */
    DEPENDENCY_TIMEOUT(504, "A required service did not respond in time");

    private final int httpStatus;

    private final String defaultMessage;

    CommonErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String defaultMessage() {
        return this.defaultMessage;
    }

    @Override
    public int httpStatus() {
        return this.httpStatus;
    }

}
