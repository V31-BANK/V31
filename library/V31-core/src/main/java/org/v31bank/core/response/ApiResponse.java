package org.v31bank.core.response;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The body every REST endpoint in the platform returns, so that a caller parses
 * one shape whether the call succeeded or failed.
 * <p>
 * A successful response carries {@link #SUCCESS_CODE} and the payload in
 * {@code data}; a failed one carries the {@link ErrorCode} and leaves
 * {@code data} unset.
 *
 * <pre class="code">
 * &#064;GetMapping("/{id}")
 * public ApiResponse&lt;CustomerResponse&gt; get(@PathVariable UUID id) {
 *     return ApiResponse.ok(CustomerResponse.from(this.customerInputPort.get(id)));
 * }
 * </pre>
 *
 * The HTTP status is set independently, and should keep its usual meaning rather
 * than being pinned to 200 — monitoring, proxies and client libraries all read
 * it, and a failure disguised as a 200 is invisible to them. The envelope carries
 * the detail the status cannot.
 * <p>
 * {@code success} is not stored as given: it is derived from the code, so an
 * instance cannot claim success while carrying a failure. Build one through the
 * factory methods rather than the canonical constructor, which exists so that a
 * service consuming another's API can deserialise a response without any
 * annotations on this type.
 * <p>
 * Null members are left in the JSON unless the service configures otherwise.
 * Set {@code spring.jackson.default-property-inclusion: non_null} so that an
 * error body is not padded with an empty payload and a success body is not
 * padded with empty error fields.
 *
 * @param success whether the call succeeded, derived from the code
 * @param code {@link #SUCCESS_CODE}, or the {@link ErrorCode} that refused the
 * request
 * @param message a description for the person waiting on the call
 * @param data the payload, absent on failure
 * @param violations the rejected fields, present only when validation failed
 * @param traceId correlates this response with the server logs of the same
 * request
 * @param timestamp when the response was produced, by the server's clock
 * @param <T> the type of the payload
 * @author Xander Wang
 * @since 0.2.0
 */
public record ApiResponse<T>(boolean success, String code, String message, T data, List<FieldViolation> violations,
        String traceId, Instant timestamp) {

    /**
     * The code carried by every successful response.
     */
    public static final String SUCCESS_CODE = "OK";

    private static final String SUCCESS_MESSAGE = "Success";

    public ApiResponse {
        Objects.requireNonNull(code, "code must not be null");
        success = SUCCESS_CODE.equals(code);
        violations = (violations != null) ? List.copyOf(violations) : null;
    }

    /**
     * Report success without a payload, for operations whose outcome is the
     * status alone.
     * @return the response
     */
    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    /**
     * Report success with a payload.
     * @param data the payload, which may be {@code null}
     * @param <T> the type of the payload
     * @return the response
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, SUCCESS_MESSAGE);
    }

    /**
     * Report success with a payload and a message meant for the person waiting on
     * the call, such as {@code "Transfer submitted, settling within 2 hours"}.
     * @param data the payload, which may be {@code null}
     * @param message the message to show
     * @param <T> the type of the payload
     * @return the response
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, SUCCESS_CODE, message, data, null, null, Instant.now());
    }

    /**
     * Report a failure using the code's own message.
     * @param errorCode the reason the request failed
     * @param <T> the type the endpoint returns when it succeeds
     * @return the response
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return error(errorCode, errorCode.defaultMessage());
    }

    /**
     * Report a failure, replacing the code's message with one describing this
     * occurrence, for example naming the identifier that was not found.
     * <p>
     * The message reaches the client, so it must not carry anything the caller is
     * not entitled to see.
     * @param errorCode the reason the request failed
     * @param message the message to report
     * @param <T> the type the endpoint returns when it succeeds
     * @return the response
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return new ApiResponse<>(false, errorCode.code(), message, null, null, null, Instant.now());
    }

    /**
     * Report {@link CommonErrorCode#VALIDATION_FAILED} together with the fields
     * that were rejected.
     * @param violations the rejected fields
     * @param <T> the type the endpoint returns when it succeeds
     * @return the response
     */
    public static <T> ApiResponse<T> invalid(List<FieldViolation> violations) {
        Objects.requireNonNull(violations, "violations must not be null");
        return new ApiResponse<>(false, CommonErrorCode.VALIDATION_FAILED.code(),
                CommonErrorCode.VALIDATION_FAILED.defaultMessage(), null, violations, null, Instant.now());
    }

    /**
     * Return this response with its payload converted, keeping the outcome, the
     * message and everything else intact.
     * <p>
     * This is what lets a layer produce the envelope around a type the next layer
     * has to replace — a use case answering with a domain object that the web
     * layer turns into a response record — without either of them rebuilding the
     * envelope and risking a different verdict. On a failure there is no payload,
     * so the converter is not called.
     * @param converter the conversion to apply to the payload
     * @param <R> the target payload type
     * @return the converted response
     */
    public <R> ApiResponse<R> map(Function<? super T, ? extends R> converter) {
        Objects.requireNonNull(converter, "converter must not be null");
        R converted = (this.data != null) ? converter.apply(this.data) : null;
        return new ApiResponse<>(this.success, this.code, this.message, converted, this.violations, this.traceId,
                this.timestamp);
    }

    /**
     * Return a copy of this response stamped with the identifier that correlates
     * it with the server logs of the same request, so that a client reporting a
     * failure hands over something searchable.
     * <p>
     * Applied centrally, in the filter or exception handler that already knows
     * the current request, rather than by each endpoint.
     * @param traceId the identifier to stamp, ignored when {@code null}
     * @return the stamped copy, or this response when there is nothing to stamp
     */
    public ApiResponse<T> withTraceId(String traceId) {
        if (traceId == null) {
            return this;
        }
        return new ApiResponse<>(this.success, this.code, this.message, this.data, this.violations, traceId,
                this.timestamp);
    }

}
