package org.v31bank.core.response;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(response.success());
        assertEquals(ApiResponse.SUCCESS_CODE, response.code());
        assertNull(response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    void okCarriesPayload() {
        ApiResponse<String> response = ApiResponse.ok("42");
        assertTrue(response.success());
        assertEquals("42", response.data());
        assertNull(response.violations());
    }

    @Test
    void okCarriesMessage() {
        ApiResponse<String> response = ApiResponse.ok("42", "Transfer submitted");
        assertEquals(ApiResponse.SUCCESS_CODE, response.code());
        assertEquals("Transfer submitted", response.message());
    }

    @Test
    void errorReportsTheCodesOwnMessage() {
        ApiResponse<String> response = ApiResponse.error(CommonErrorCode.NOT_FOUND);
        assertFalse(response.success());
        assertEquals("NOT_FOUND", response.code());
        assertEquals(CommonErrorCode.NOT_FOUND.defaultMessage(), response.message());
        assertNull(response.data());
    }

    @Test
    void errorReplacesTheMessage() {
        ApiResponse<String> response = ApiResponse.error(CommonErrorCode.NOT_FOUND, "No wallet exists with id 7");
        assertEquals("NOT_FOUND", response.code());
        assertEquals("No wallet exists with id 7", response.message());
    }

    @Test
    void errorRejectsMissingCode() {
        assertThrows(NullPointerException.class, () -> ApiResponse.error(null));
    }

    @Test
    void invalidReportsTheRejectedFields() {
        FieldViolation violation = new FieldViolation("beneficiary.iban", "must be a valid IBAN");
        ApiResponse<String> response = ApiResponse.invalid(List.of(violation));
        assertFalse(response.success());
        assertEquals("VALIDATION_FAILED", response.code());
        assertEquals(List.of(violation), response.violations());
    }

    @Test
    void violationsAreCopiedAndUnmodifiable() {
        List<FieldViolation> violations = new ArrayList<>(List.of(new FieldViolation("amount", "must be positive")));
        ApiResponse<String> response = ApiResponse.invalid(violations);
        violations.clear();
        assertEquals(1, response.violations().size());
        assertThrows(UnsupportedOperationException.class,
                () -> response.violations().add(new FieldViolation("x", "y")));
    }

    @Test
    void cannotClaimSuccessWhileCarryingAFailure() {
        ApiResponse<String> lying = new ApiResponse<>(true, "NOT_FOUND", "gone", null, null, null, null);
        assertFalse(lying.success());
    }

    @Test
    void cannotDenySuccessWhileCarryingTheSuccessCode() {
        ApiResponse<String> lying = new ApiResponse<>(false, ApiResponse.SUCCESS_CODE, "fine", "42", null, null, null);
        assertTrue(lying.success());
    }

    @Test
    void withTraceIdCopiesRatherThanMutates() {
        ApiResponse<String> response = ApiResponse.ok("42");
        ApiResponse<String> stamped = response.withTraceId("d4f1");
        assertNull(response.traceId());
        assertEquals("d4f1", stamped.traceId());
        assertEquals("42", stamped.data());
        assertEquals(response.timestamp(), stamped.timestamp());
    }

    @Test
    void withTraceIdKeepsTheResponseWhenThereIsNothingToStamp() {
        ApiResponse<String> response = ApiResponse.ok("42");
        assertSame(response, response.withTraceId(null));
    }

    @Test
    void mapConvertsThePayloadAndKeepsTheVerdict() {
        ApiResponse<Integer> mapped = ApiResponse.ok("42", "Done").withTraceId("d4f1").map(Integer::parseInt);
        assertEquals(42, mapped.data());
        assertTrue(mapped.success());
        assertEquals("OK", mapped.code());
        assertEquals("Done", mapped.message());
        assertEquals("d4f1", mapped.traceId());
    }

    @Test
    void mapLeavesAFailureAlone() {
        ApiResponse<String> failure = ApiResponse.error(CommonErrorCode.NOT_FOUND, "gone");
        ApiResponse<Integer> mapped = failure.map((value) -> {
            throw new AssertionError("the converter must not run when there is no payload");
        });
        assertFalse(mapped.success());
        assertEquals("NOT_FOUND", mapped.code());
        assertEquals("gone", mapped.message());
        assertNull(mapped.data());
    }

    @Test
    void mapRejectsAMissingConverter() {
        assertThrows(NullPointerException.class, () -> ApiResponse.ok("42").map(null));
    }

}
