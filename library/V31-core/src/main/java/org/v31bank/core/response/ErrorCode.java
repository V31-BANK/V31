package org.v31bank.core.response;

/**
 * A machine-readable reason a request failed, stable enough for callers to branch
 * on.
 * <p>
 * Implementations are expected to be enums, one per service, so that the set of
 * failures a service can report is enumerable and reviewable in a single file.
 * {@link CommonErrorCode} covers the failures every service shares; a service
 * declares its own enum for the ones only it can produce.
 * <p>
 * The code, not the message, is the contract: messages are written for humans and
 * may be reworded at any time, so a caller that matches on message text will
 * break.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface ErrorCode {

    /**
     * Return the identifier callers branch on, unique across the platform and
     * conventionally {@code UPPER_SNAKE_CASE}.
     * @return the code
     */
    String code();

    /**
     * Return the message to report when the caller supplies none. It reaches
     * clients, so it must describe the failure without disclosing internals —
     * never a stack trace, a SQL statement, or an upstream provider's name.
     * @return the default message
     */
    String defaultMessage();

    /**
     * Return the HTTP status to answer with, which is what lets one exception
     * handler serve every error code without a per-service mapping table.
     * <p>
     * Defaults to {@code 422 Unprocessable Content}: a business rule refused a
     * request that was itself well formed, which is the common case for a code
     * declared by a service.
     * @return the HTTP status code
     */
    default int httpStatus() {
        return 422;
    }

}
