package org.v31bank.core.constant;

/**
 * Names of the HTTP headers the platform reads and forwards, kept in one place so
 * that the filter setting a header and the client sending it cannot drift apart
 * over a spelling.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class ApiHeaders {

    /**
     * Identifier correlating everything done while handling one request: the log
     * lines it wrote, the calls it made downstream, and the {@code traceId} on the
     * response. Accepted from the caller when present so a trace survives across
     * service boundaries, and generated otherwise.
     */
    public static final String REQUEST_ID = "X-Request-Id";

    /**
     * Key making a write safe to retry. A caller that never learns the outcome of
     * a request — a timeout, a dropped connection — repeats it with the same key,
     * and the service replays the original response instead of moving the money a
     * second time.
     * <p>
     * Named after the header established by payment providers and specified in
     * {@code draft-ietf-httpapi-idempotency-key-header}.
     */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private ApiHeaders() {
    }

}
