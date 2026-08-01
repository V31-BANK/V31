package org.v31bank.data.valkey.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ValkeyKeys}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ValkeyKeysTests {

    private final ValkeyKeys keys = new ValkeyKeys("v31");

    @Test
    void putsEverythingUnderThePrefix() {
        assertEquals("v31:customer:0199-7a", this.keys.of("customer", "0199-7a"));
        assertEquals("v31:session", this.keys.of("session"));
    }

    @Test
    void buildsAPatternForSweepingANamespace() {
        assertEquals("v31:customer:*", this.keys.patternUnder("customer"));
    }

    @Test
    void refusesASegmentThatWouldEscapeItsNamespace() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> this.keys.of("session", "7:session:admin"));
        assertTrue(ex.getMessage().contains("must not contain ':'"), ex.getMessage());
    }

    @Test
    void refusesAnEmptySegment() {
        assertThrows(IllegalArgumentException.class, () -> this.keys.of("customer", ""));
        assertThrows(IllegalArgumentException.class, () -> this.keys.of("customer", "  "));
        assertThrows(NullPointerException.class, () -> this.keys.of("customer", null));
    }

    @Test
    void refusesAKeyWithNothingBelowThePrefix() {
        assertThrows(IllegalArgumentException.class, this.keys::of);
    }

    @Test
    void refusesAPrefixThatIsNotOne() {
        assertThrows(IllegalArgumentException.class, () -> new ValkeyKeys(""));
        assertThrows(IllegalArgumentException.class, () -> new ValkeyKeys("v31:cache"));
        assertThrows(NullPointerException.class, () -> new ValkeyKeys(null));
    }

    @Test
    void reportsItsPrefix() {
        assertEquals("v31", this.keys.getPrefix());
    }

}
