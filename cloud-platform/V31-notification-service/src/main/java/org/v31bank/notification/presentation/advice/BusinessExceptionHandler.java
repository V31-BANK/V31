package org.v31bank.notification.presentation.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.v31bank.core.exception.BusinessException;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;

/**
 * Turns every failure that escapes a controller into the platform's response
 * envelope.
 * <p>
 * Needed here because this service's work is calling another one: the ledger
 * reports a refusal as a gRPC status, the adapter turns it into a
 * {@link BusinessException}, and without this it would surface as a {@code 500}
 * with Spring's default body — telling a caller that this service broke, when in
 * fact the ledger refused the request for a reason the caller can act on.
 * <p>
 * Extending {@link ResponseEntityExceptionHandler} rather than catching
 * {@link Exception} alone is deliberate: Spring raises its own exceptions for a
 * path variable that will not parse, an unknown route, an unreadable body. A
 * blanket handler answers all of them with {@code 500}, which is both wrong and,
 * to anything watching the error rate, alarming.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestControllerAdvice
public class BusinessExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BusinessExceptionHandler.class);

    /**
     * Answer a request the ledger refused, keeping the code it reported.
     * @param ex the refusal
     * @return the response to send
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.debug("Refused with {}", errorCode.code(), ex);
        return ResponseEntity.status(statusOf(errorCode)).body(ApiResponse.error(errorCode, ex.getMessage()));
    }

    /**
     * Answer anything unrecognised without disclosing why.
     * @param ex the failure
     * @return the response to send
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unhandled failure", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(CommonErrorCode.INTERNAL_ERROR));
    }

    /**
     * Replace the {@code ProblemDetail} Spring would otherwise send with the
     * platform's envelope, leaving the status it chose alone.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        log.debug("Rejected with {}: {}", statusCode, ex.getMessage());
        Object envelope = (body instanceof ApiResponse) ? body : ApiResponse.error(errorCodeFor(statusCode));
        return super.handleExceptionInternal(ex, envelope, headers, statusCode, request);
    }

    /**
     * Place a failure the ledger reported, defaulting to what an
     * {@link ErrorCode} declares when the code came from a service whose enum this
     * build does not have.
     * @param errorCode the code the failure carried
     * @return the status to answer with
     */
    private static int statusOf(ErrorCode errorCode) {
        int status = errorCode.httpStatus();
        return (status >= 100 && status < 600) ? status : CommonErrorCode.UNPROCESSABLE.httpStatus();
    }

    /**
     * Map the status Spring selected onto the code that says the same thing, so
     * that a caller branching on {@code code} does not have to read the status for
     * the failures the framework reports.
     * <p>
     * A method that is not allowed and a body that cannot be read fall through to
     * the client-error default: {@link CommonErrorCode} has no member for either,
     * and the status Spring chose already says which it was.
     * @param status the status Spring chose
     * @return the matching error code
     */
    private static ErrorCode errorCodeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> CommonErrorCode.VALIDATION_FAILED;
            case 401 -> CommonErrorCode.UNAUTHENTICATED;
            case 403 -> CommonErrorCode.FORBIDDEN;
            case 404 -> CommonErrorCode.NOT_FOUND;
            case 409 -> CommonErrorCode.CONFLICT;
            case 422 -> CommonErrorCode.UNPROCESSABLE;
            case 429 -> CommonErrorCode.RATE_LIMITED;
            default -> status.is4xxClientError() ? CommonErrorCode.VALIDATION_FAILED : CommonErrorCode.INTERNAL_ERROR;
        };
    }

}
