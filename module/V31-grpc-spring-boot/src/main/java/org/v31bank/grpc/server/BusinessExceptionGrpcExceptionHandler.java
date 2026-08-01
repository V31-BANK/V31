package org.v31bank.grpc.server;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import org.v31bank.core.exception.BusinessException;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.grpc.status.GrpcStatuses;

/**
 * Reports a refused request over gRPC the way the REST layer reports it over
 * HTTP: with a status the transport understands and the exact code beside it.
 * <p>
 * Spring gRPC collects every {@link GrpcExceptionHandler} bean into one
 * interceptor and asks them in order until one answers, so this takes the
 * failures it owns and returns {@code null} for everything else.
 * <p>
 * The message is passed through, because a {@link BusinessException} carries one
 * written for the caller. That is not true of anything else, which is why
 * {@link UnexpectedExceptionGrpcExceptionHandler} does not do the same.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class BusinessExceptionGrpcExceptionHandler implements GrpcExceptionHandler {

    @Override
    public StatusException handleException(Throwable exception) {
        if (!(exception instanceof BusinessException businessException)) {
            return null;
        }
        ErrorCode errorCode = businessException.getErrorCode();
        Metadata trailers = new Metadata();
        trailers.put(GrpcStatuses.ERROR_CODE, errorCode.code());
        return GrpcStatuses.statusCodeFor(errorCode)
            .toStatus()
            .withDescription(businessException.getMessage())
            .asException(trailers);
    }

}
