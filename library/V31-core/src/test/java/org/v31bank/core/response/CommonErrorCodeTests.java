package org.v31bank.core.response;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link CommonErrorCode}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CommonErrorCodeTests {

    @Test
    void reportsItsNameAsTheWireCode() {
        assertEquals("NOT_FOUND", CommonErrorCode.NOT_FOUND.code());
        assertEquals(404, CommonErrorCode.NOT_FOUND.httpStatus());
    }

    @Test
    void findsACodeByItsWireForm() {
        assertEquals(Optional.of(CommonErrorCode.CONFLICT), CommonErrorCode.find("CONFLICT"));
        assertEquals(Optional.of(CommonErrorCode.UNPROCESSABLE), CommonErrorCode.find("UNPROCESSABLE"));
    }

    @Test
    void doesNotFindWhatItDoesNotDeclare() {
        assertEquals(Optional.empty(), CommonErrorCode.find("CATEGORY_HAS_CHILDREN"));
        assertEquals(Optional.empty(), CommonErrorCode.find("conflict"));
        assertEquals(Optional.empty(), CommonErrorCode.find(""));
        assertEquals(Optional.empty(), CommonErrorCode.find(null));
    }

}
