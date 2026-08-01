package org.v31bank.grpc.client;

import java.util.Objects;
import java.util.function.Supplier;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import org.v31bank.core.exception.BusinessException;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.grpc.status.GrpcStatuses;
import org.v31bank.grpc.status.RemoteErrorCode;

/**
 * Turns a failed call into the same {@link BusinessException} the far side threw.
 * <p>
 * Without this, a service calling another has to reason about
 * {@link StatusRuntimeException} and status codes in the middle of its
 * application layer, and translate them into its own outcomes by hand at every
 * call site. With it, a remote refusal arrives as the kind of exception the rest
 * of the platform already deals with, carrying the code the far side reported.
 *
 * <h2>Why this is not an interceptor</h2>
 *
 * A {@link io.grpc.ClientInterceptor} can change the status a call fails with,
 * but not the type of exception the generated stub raises from it — the stub
 * builds a {@code StatusRuntimeException} and there is no seam for anything else.
 * So the translation happens where the call is made, which also keeps it visible:
 * a reader can see that a remote failure is being converted rather than having to
 * know that a channel was configured to do it invisibly.
 *
 * <pre class="code">
 * CustomerResponse response = GrpcErrors.call(() -&gt; this.customerStub.get(request));
 * </pre>
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class GrpcErrors {

    private GrpcErrors() {
    }

    /**
     * Make a call, reporting a failure as a {@link BusinessException}.
     * @param call the call to make
     * @param <T> what the call returns
     * @return what the call returned
     * @throws BusinessException if the call failed
     */
    public static <T> T call(Supplier<T> call) {
        Objects.requireNonNull(call, "call must not be null");
        try {
            return call.get();
        }
        catch (StatusRuntimeException ex) {
            throw asBusinessException(ex);
        }
    }

    /**
     * Translate a failed call.
     * <p>
     * A failure from a V31 service carries its exact code in the trailers and
     * that is used as it stands. A failure from anything else — a proxy that ran
     * out of capacity, a mesh that could not route the call, a deadline the
     * client itself enforced — has no such trailer, so the code is derived from
     * the gRPC status instead. Either way the caller receives an
     * {@link ErrorCode}.
     * <p>
     * The original exception is kept as the cause: the status, the trailers and
     * the stack all stay reachable for the logs.
     * @param exception the failure to translate
     * @return the exception to raise in its place
     */
    public static BusinessException asBusinessException(StatusRuntimeException exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        Status status = exception.getStatus();
        String remoteCode = (exception.getTrailers() != null)
                ? exception.getTrailers().get(GrpcStatuses.ERROR_CODE) : null;
        if (remoteCode == null) {
            // Nothing on the far side chose to say this: the failure came from the
            // transport, a proxy, or a deadline this client enforced. Its
            // description is diagnostic text — "io exception", the name of a
            // resolver — written for whoever reads the logs and meaningless, or
            // worse, revealing, to whoever made the call. The exception carries it
            // as the cause; the message does not.
            ErrorCode common = GrpcStatuses.commonErrorCodeFor(status.getCode());
            return new BusinessException(common, common.defaultMessage(), exception);
        }
        // A V31 service refused this deliberately, so the description is a message
        // it wrote for the caller.
        String message = (status.getDescription() != null) ? status.getDescription()
                : GrpcStatuses.commonErrorCodeFor(status.getCode()).defaultMessage();
        return new BusinessException(new RemoteErrorCode(remoteCode, message,
                GrpcStatuses.httpStatusFor(status.getCode())), message, exception);
    }

}
