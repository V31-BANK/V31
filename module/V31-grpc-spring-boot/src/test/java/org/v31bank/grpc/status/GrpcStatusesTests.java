/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.v31bank.grpc.status;

import io.grpc.Status;
import org.junit.jupiter.api.Test;

import org.v31bank.core.response.CommonErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrpcStatuses}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class GrpcStatusesTests {

	@Test
	void mapsTheCommonCodesOntoTheStatusThatSaysTheSameThing() {
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.VALIDATION_FAILED))
			.isEqualTo(Status.Code.INVALID_ARGUMENT);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.UNAUTHENTICATED)).isEqualTo(Status.Code.UNAUTHENTICATED);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.FORBIDDEN)).isEqualTo(Status.Code.PERMISSION_DENIED);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.NOT_FOUND)).isEqualTo(Status.Code.NOT_FOUND);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.CONFLICT)).isEqualTo(Status.Code.ALREADY_EXISTS);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.UNPROCESSABLE))
			.isEqualTo(Status.Code.FAILED_PRECONDITION);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.RATE_LIMITED)).isEqualTo(Status.Code.RESOURCE_EXHAUSTED);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.DEPENDENCY_UNAVAILABLE))
			.isEqualTo(Status.Code.UNAVAILABLE);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.DEPENDENCY_TIMEOUT))
			.isEqualTo(Status.Code.DEADLINE_EXCEEDED);
		assertThat(GrpcStatuses.statusCodeFor(CommonErrorCode.INTERNAL_ERROR)).isEqualTo(Status.Code.INTERNAL);
	}

	@Test
	void mapsACodeItHasNeverSeenOntoInternal() {
		assertThat(GrpcStatuses.statusCodeFor(new RemoteErrorCode("ODD", "odd", 418))).isEqualTo(Status.Code.INTERNAL);
	}

	@Test
	void comesBackToTheSameCommonCodeItStartedFrom() {
		for (CommonErrorCode errorCode : CommonErrorCode.values()) {
			Status.Code status = GrpcStatuses.statusCodeFor(errorCode);
			assertThat(GrpcStatuses.httpStatusFor(status))
				.as(errorCode + " should survive the round trip through " + status)
				.isEqualTo(errorCode.httpStatus());
		}
	}

	@Test
	void derivesACodeForAFailureFromSomethingThatIsNotAV31Service() {
		assertThat(GrpcStatuses.commonErrorCodeFor(Status.Code.UNAVAILABLE))
			.isEqualTo(CommonErrorCode.DEPENDENCY_UNAVAILABLE);
		assertThat(GrpcStatuses.commonErrorCodeFor(Status.Code.DEADLINE_EXCEEDED))
			.isEqualTo(CommonErrorCode.DEPENDENCY_TIMEOUT);
		assertThat(GrpcStatuses.commonErrorCodeFor(Status.Code.UNKNOWN)).isEqualTo(CommonErrorCode.INTERNAL_ERROR);
		assertThat(GrpcStatuses.commonErrorCodeFor(Status.Code.DATA_LOSS)).isEqualTo(CommonErrorCode.INTERNAL_ERROR);
	}

}
