package org.v31bank.grpc.server;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.grpc.status.GrpcStatuses;

/**
 * Answers anything that reached the transport unrecognised.
 * <p>
 * Asked last, after every handler that owns a particular failure has declined.
 * Without it the framework falls back to {@code UNKNOWN} with nothing attached,
 * which tells a caller only that something went wrong and leaves it with no code
 * to branch on and no thread to pull in the logs.
 *
 * <h2>The message is not passed through</h2>
 *
 * An exception raised by a driver, a serializer or a null dereference carries a
 * message written for whoever reads the logs, and it routinely quotes a fragment
 * of the request, a column name, or a class that failed. Sending that to a caller
 * discloses how the service is built to anybody able to provoke an error. So the
 * caller is told only that the call did not complete, and the cause is logged
 * here — reachable through the request identifier the call was carrying.
 * <p>
 * A {@link StatusRuntimeException} raised deliberately by service code is left
 * alone: it was already phrased for the wire.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class UnexpectedExceptionGrpcExceptionHandler implements GrpcExceptionHandler {

    private static final Log logger = LogFactory.getLog(UnexpectedExceptionGrpcExceptionHandler.class);

    @Override
    public StatusException handleException(Throwable exception) {
        if (exception instanceof StatusException statusException) {
            return statusException;
        }
        if (exception instanceof StatusRuntimeException statusRuntimeException) {
            return new StatusException(statusRuntimeException.getStatus(), statusRuntimeException.getTrailers());
        }
        logger.error("Unhandled failure while serving a gRPC call", exception);
        Metadata trailers = new Metadata();
        trailers.put(GrpcStatuses.ERROR_CODE, CommonErrorCode.INTERNAL_ERROR.code());
        return Status.INTERNAL.withDescription(CommonErrorCode.INTERNAL_ERROR.defaultMessage())
            .asException(trailers);
    }

}
