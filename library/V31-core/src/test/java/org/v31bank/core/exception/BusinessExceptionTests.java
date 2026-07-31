package org.v31bank.core.exception;

import org.junit.jupiter.api.Test;

import org.v31bank.core.response.CommonErrorCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link BusinessException}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class BusinessExceptionTests {

    @Test
    void fallsBackToTheCodesOwnMessage() {
        BusinessException exception = new BusinessException(CommonErrorCode.CONFLICT);
        assertEquals(CommonErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals(CommonErrorCode.CONFLICT.defaultMessage(), exception.getMessage());
    }

    @Test
    void carriesTheMessageDescribingTheOccurrence() {
        BusinessException exception = new BusinessException(CommonErrorCode.NOT_FOUND, "No wallet exists with id 7");
        assertEquals("No wallet exists with id 7", exception.getMessage());
    }

    @Test
    void keepsTheFailureItWasTranslatedFrom() {
        IllegalStateException cause = new IllegalStateException("unique constraint violated");
        BusinessException exception = new BusinessException(CommonErrorCode.CONFLICT, "Code already in use", cause);
        assertSame(cause, exception.getCause());
        assertEquals(409, exception.getErrorCode().httpStatus());
    }

    @Test
    void rejectsMissingCode() {
        assertThrows(NullPointerException.class, () -> new BusinessException(null, "boom"));
        assertThrows(NullPointerException.class, () -> new BusinessException(null));
    }

}
