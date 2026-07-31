package org.v31bank.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link Masks}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class MasksTests {

    @Test
    void keepsTheLastFourDigitsOfAnAccountNumber() {
        assertEquals("****6789", Masks.accountNumber("4111111111116789"));
        assertEquals("****4567", Masks.phoneNumber("+41791234567"));
    }

    @Test
    void hidesAValueTooShortToMaskSafely() {
        assertEquals("****", Masks.accountNumber("1234567"));
        assertEquals("****", Masks.accountNumber("12"));
        assertEquals("****", Masks.accountNumber(""));
    }

    @Test
    void doesNotDiscloseHowLongTheValueWas() {
        assertEquals(Masks.accountNumber("11116789"), Masks.accountNumber("11111111111111116789"));
        assertEquals(8, Masks.accountNumber("11111111111111116789").length());
    }

    @Test
    void keepsAnEmailRecognisableWithoutHoldingIt() {
        assertEquals("x****@v31bank.org", Masks.email("xander.wang@v31bank.org"));
    }

    @Test
    void hidesAnythingThatIsNotAnEmail() {
        assertEquals("****", Masks.email("not an address"));
        assertEquals("****", Masks.email("@v31bank.org"));
        assertEquals("****", Masks.email("xander@"));
    }

    @Test
    void shortensABlockchainAddressFromBothEnds() {
        assertEquals("bc1qar****5mdq", Masks.cryptoAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"));
        assertEquals("****", Masks.cryptoAddress("bc1qar"));
    }

    @Test
    void hidesASecretEntirely() {
        assertEquals("****", Masks.secret("sk_live_51H8xQ2eZvKYlo2C"));
    }

    @Test
    void passesNullThroughSoLoggingNeverFails() {
        assertNull(Masks.accountNumber(null));
        assertNull(Masks.email(null));
        assertNull(Masks.cryptoAddress(null));
        assertNull(Masks.secret(null));
    }

}
