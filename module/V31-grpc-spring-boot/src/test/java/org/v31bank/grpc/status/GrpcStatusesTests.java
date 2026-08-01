package org.v31bank.grpc.status;

import io.grpc.Status;

import org.junit.jupiter.api.Test;

import org.v31bank.core.response.CommonErrorCode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link GrpcStatuses}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class GrpcStatusesTests {

    @Test
    void mapsTheCommonCodesOntoTheStatusThatSaysTheSameThing() {
        assertEquals(Status.Code.INVALID_ARGUMENT, GrpcStatuses.statusCodeFor(CommonErrorCode.VALIDATION_FAILED));
        assertEquals(Status.Code.UNAUTHENTICATED, GrpcStatuses.statusCodeFor(CommonErrorCode.UNAUTHENTICATED));
        assertEquals(Status.Code.PERMISSION_DENIED, GrpcStatuses.statusCodeFor(CommonErrorCode.FORBIDDEN));
        assertEquals(Status.Code.NOT_FOUND, GrpcStatuses.statusCodeFor(CommonErrorCode.NOT_FOUND));
        assertEquals(Status.Code.ALREADY_EXISTS, GrpcStatuses.statusCodeFor(CommonErrorCode.CONFLICT));
        assertEquals(Status.Code.FAILED_PRECONDITION, GrpcStatuses.statusCodeFor(CommonErrorCode.UNPROCESSABLE));
        assertEquals(Status.Code.RESOURCE_EXHAUSTED, GrpcStatuses.statusCodeFor(CommonErrorCode.RATE_LIMITED));
        assertEquals(Status.Code.UNAVAILABLE, GrpcStatuses.statusCodeFor(CommonErrorCode.DEPENDENCY_UNAVAILABLE));
        assertEquals(Status.Code.DEADLINE_EXCEEDED, GrpcStatuses.statusCodeFor(CommonErrorCode.DEPENDENCY_TIMEOUT));
        assertEquals(Status.Code.INTERNAL, GrpcStatuses.statusCodeFor(CommonErrorCode.INTERNAL_ERROR));
    }

    @Test
    void mapsACodeItHasNeverSeenOntoInternal() {
        assertEquals(Status.Code.INTERNAL, GrpcStatuses.statusCodeFor(new RemoteErrorCode("ODD", "odd", 418)));
    }

    @Test
    void comesBackToTheSameCommonCodeItStartedFrom() {
        for (CommonErrorCode errorCode : CommonErrorCode.values()) {
            Status.Code status = GrpcStatuses.statusCodeFor(errorCode);
            assertEquals(errorCode.httpStatus(), GrpcStatuses.httpStatusFor(status),
                    errorCode + " should survive the round trip through " + status);
        }
    }

    @Test
    void derivesACodeForAFailureFromSomethingThatIsNotAV31Service() {
        assertEquals(CommonErrorCode.DEPENDENCY_UNAVAILABLE, GrpcStatuses.commonErrorCodeFor(Status.Code.UNAVAILABLE));
        assertEquals(CommonErrorCode.DEPENDENCY_TIMEOUT,
                GrpcStatuses.commonErrorCodeFor(Status.Code.DEADLINE_EXCEEDED));
        assertEquals(CommonErrorCode.INTERNAL_ERROR, GrpcStatuses.commonErrorCodeFor(Status.Code.UNKNOWN));
        assertEquals(CommonErrorCode.INTERNAL_ERROR, GrpcStatuses.commonErrorCodeFor(Status.Code.DATA_LOSS));
    }

}
