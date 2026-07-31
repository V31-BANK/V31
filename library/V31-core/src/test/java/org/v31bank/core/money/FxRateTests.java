package org.v31bank.core.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FxRate}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class FxRateTests {

    private static final Instant OBSERVED_AT = Instant.parse("2026-08-01T09:00:00Z");

    @Test
    void convertsIntoTheQuoteAsset() {
        FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("60000.00"), OBSERVED_AT);
        assertEquals(Money.of(Asset.USD, "30000.00"), rate.convert(Money.of(Asset.BTC, "0.5")));
    }

    @Test
    void roundsNeutrallyByDefault() {
        FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("2.005"), OBSERVED_AT);
        assertEquals(Money.of(Asset.USD, "2.00"), rate.convert(Money.of(Asset.BTC, "1")));
    }

    @Test
    void roundsAsAsked() {
        FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("2.005"), OBSERVED_AT);
        assertEquals(Money.of(Asset.USD, "2.01"), rate.convert(Money.of(Asset.BTC, "1"), RoundingMode.UP));
    }

    @Test
    void refusesToConvertTheWrongWayRound() {
        FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("60000.00"), OBSERVED_AT);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rate.convert(Money.of(Asset.USD, "100.00")));
        assertTrue(ex.getMessage().contains("A BTC/USD rate converts BTC, not USD"), ex.getMessage());
    }

    @Test
    void refusesARateAgainstItself() {
        assertThrows(IllegalArgumentException.class,
                () -> new FxRate(Asset.USD, Asset.USD, BigDecimal.ONE, OBSERVED_AT));
    }

    @Test
    void refusesARateThatIsNotPositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new FxRate(Asset.BTC, Asset.USD, BigDecimal.ZERO, OBSERVED_AT));
        assertThrows(IllegalArgumentException.class,
                () -> new FxRate(Asset.BTC, Asset.USD, new BigDecimal("-1"), OBSERVED_AT));
    }

    @Test
    void invertsTheDirection() {
        FxRate inverse = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("50000"), OBSERVED_AT).inverse();
        assertEquals(Asset.USD, inverse.base());
        assertEquals(Asset.BTC, inverse.quote());
        assertEquals(Money.of(Asset.BTC, "1"), inverse.convert(Money.of(Asset.USD, "50000.00")));
    }

    @Test
    void reportsWhenItHasGoneStale() {
        FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("60000.00"), OBSERVED_AT);
        assertFalse(rate.isOlderThan(Duration.ofMinutes(1), OBSERVED_AT.plusSeconds(30)));
        assertTrue(rate.isOlderThan(Duration.ofMinutes(1), OBSERVED_AT.plusSeconds(90)));
    }

    @Test
    void readsAsAQuote() {
        assertEquals("1 BTC = 60000.00 USD",
                new FxRate(Asset.BTC, Asset.USD, new BigDecimal("60000.00"), OBSERVED_AT).toString());
    }

}
