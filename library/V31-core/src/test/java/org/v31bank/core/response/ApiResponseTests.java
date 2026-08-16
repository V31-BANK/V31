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

package org.v31bank.core.response;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ApiResponse}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ApiResponseTests {

	@Test
	void okWithoutPayloadReportsSuccess() {
		ApiResponse<Void> response = ApiResponse.ok();
		assertThat(response.success()).isTrue();
		assertThat(response.code()).isEqualTo(ApiResponse.SUCCESS_CODE);
		assertThat(response.data()).isNull();
		assertThat(response.timestamp()).isNotNull();
	}

	@Test
	void okCarriesPayload() {
		ApiResponse<String> response = ApiResponse.ok("42");
		assertThat(response.success()).isTrue();
		assertThat(response.data()).isEqualTo("42");
		assertThat(response.violations()).isNull();
	}

	@Test
	void okCarriesMessage() {
		ApiResponse<String> response = ApiResponse.ok("42", "Transfer submitted");
		assertThat(response.code()).isEqualTo(ApiResponse.SUCCESS_CODE);
		assertThat(response.message()).isEqualTo("Transfer submitted");
	}

	@Test
	void errorReportsTheCodesOwnMessage() {
		ApiResponse<String> response = ApiResponse.error(CommonErrorCode.NOT_FOUND);
		assertThat(response.success()).isFalse();
		assertThat(response.code()).isEqualTo("NOT_FOUND");
		assertThat(response.message()).isEqualTo(CommonErrorCode.NOT_FOUND.defaultMessage());
		assertThat(response.data()).isNull();
	}

	@Test
	void errorReplacesTheMessage() {
		ApiResponse<String> response = ApiResponse.error(CommonErrorCode.NOT_FOUND, "No wallet exists with id 7");
		assertThat(response.code()).isEqualTo("NOT_FOUND");
		assertThat(response.message()).isEqualTo("No wallet exists with id 7");
	}

	@Test
	void errorRejectsMissingCode() {
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> ApiResponse.error(null));
	}

	@Test
	void invalidReportsTheRejectedFields() {
		FieldViolation violation = new FieldViolation("beneficiary.iban", "must be a valid IBAN");
		ApiResponse<String> response = ApiResponse.invalid(List.of(violation));
		assertThat(response.success()).isFalse();
		assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
		assertThat(response.violations()).containsExactly(violation);
	}

	@Test
	void violationsAreCopiedAndUnmodifiable() {
		List<FieldViolation> violations = new ArrayList<>(List.of(new FieldViolation("amount", "must be positive")));
		ApiResponse<String> response = ApiResponse.invalid(violations);
		violations.clear();
		assertThat(response.violations()).hasSize(1);
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> response.violations().add(new FieldViolation("x", "y")));
	}

	@Test
	void cannotClaimSuccessWhileCarryingAFailure() {
		ApiResponse<String> lying = new ApiResponse<>(true, "NOT_FOUND", "gone", null, null, null, null);
		assertThat(lying.success()).isFalse();
	}

	@Test
	void cannotDenySuccessWhileCarryingTheSuccessCode() {
		ApiResponse<String> lying = new ApiResponse<>(false, ApiResponse.SUCCESS_CODE, "fine", "42", null, null, null);
		assertThat(lying.success()).isTrue();
	}

	@Test
	void withTraceIdCopiesRatherThanMutates() {
		ApiResponse<String> response = ApiResponse.ok("42");
		ApiResponse<String> stamped = response.withTraceId("d4f1");
		assertThat(response.traceId()).isNull();
		assertThat(stamped.traceId()).isEqualTo("d4f1");
		assertThat(stamped.data()).isEqualTo("42");
		assertThat(stamped.timestamp()).isEqualTo(response.timestamp());
	}

	@Test
	void withTraceIdKeepsTheResponseWhenThereIsNothingToStamp() {
		ApiResponse<String> response = ApiResponse.ok("42");
		assertThat(response.withTraceId(null)).isSameAs(response);
	}

	@Test
	void mapConvertsThePayloadAndKeepsTheVerdict() {
		ApiResponse<Integer> mapped = ApiResponse.ok("42", "Done").withTraceId("d4f1").map(Integer::parseInt);
		assertThat(mapped.data()).isEqualTo(42);
		assertThat(mapped.success()).isTrue();
		assertThat(mapped.code()).isEqualTo("OK");
		assertThat(mapped.message()).isEqualTo("Done");
		assertThat(mapped.traceId()).isEqualTo("d4f1");
	}

	@Test
	void mapLeavesAFailureAlone() {
		ApiResponse<String> failure = ApiResponse.error(CommonErrorCode.NOT_FOUND, "gone");
		ApiResponse<Integer> mapped = failure.map((value) -> {
			throw new AssertionError("the converter must not run when there is no payload");
		});
		assertThat(mapped.success()).isFalse();
		assertThat(mapped.code()).isEqualTo("NOT_FOUND");
		assertThat(mapped.message()).isEqualTo("gone");
		assertThat(mapped.data()).isNull();
	}

	@Test
	void mapRejectsAMissingConverter() {
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> ApiResponse.ok("42").map(null));
	}

}
