package org.v31bank.core.money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Money}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class MoneyTests {

    @Test
    void refusesPrecisionTheAssetCannotHold() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Money.of(Asset.USD, new BigDecimal("1.005")));
        assertTrue(ex.getMessage().contains("USD cannot hold 1.005 exactly"), ex.getMessage());
    }

    @Test
    void refusesASatoshiTooFar() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(Asset.BTC, "0.000000001"));
    }

    @Test
    void padsToTheAssetsScale() {
        assertEquals(new BigDecimal("1.50"), Money.of(Asset.USD, "1.5").getAmount());
        assertEquals(new BigDecimal("1.00000000"), Money.of(Asset.BTC, "1").getAmount());
    }

    @Test
    void roundsOnlyWhenAskedTo() {
        Money rounded = Money.of(Asset.USD, new BigDecimal("1.005"), RoundingMode.HALF_EVEN);
        assertEquals(new BigDecimal("1.00"), rounded.getAmount());
    }

    @Test
    void rejectsTextThatIsNotAnAmount() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(Asset.USD, "not money"));
    }

    @Test
    void holdsAnEtherBalanceInWei() {
        Money ether = Money.of(Asset.ETH, "1");
        assertEquals(new BigInteger("1000000000000000000"), ether.toMinorUnits());
        assertEquals(ether, Money.ofMinorUnits(Asset.ETH, new BigInteger("1000000000000000000")));
    }

    @Test
    void holdsASingleSatoshi() {
        Money satoshi = Money.ofMinorUnits(Asset.BTC, 1);
        assertEquals(new BigDecimal("0.00000001"), satoshi.getAmount());
        assertEquals(BigInteger.ONE, satoshi.toMinorUnits());
    }

    @Test
    void adds() {
        assertEquals(Money.of(Asset.USD, "3.75"), Money.of(Asset.USD, "1.25").add(Money.of(Asset.USD, "2.50")));
    }

    @Test
    void addsWithoutBinaryFloatingPointError() {
        assertEquals(Money.of(Asset.USD, "0.30"), Money.of(Asset.USD, "0.10").add(Money.of(Asset.USD, "0.20")));
    }

    @Test
    void subtractsPastZero() {
        Money owed = Money.of(Asset.USD, "1.00").subtract(Money.of(Asset.USD, "2.50"));
        assertEquals(Money.of(Asset.USD, "-1.50"), owed);
        assertTrue(owed.isNegative());
    }

    @Test
    void refusesToMixAssets() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Money.of(Asset.BTC, "1").add(Money.of(Asset.USD, "1")));
        assertTrue(ex.getMessage().contains("crossing assets needs an explicit FxRate"), ex.getMessage());
    }

    @Test
    void multipliesAFee() {
        Money fee = Money.of(Asset.USD, "100.00").multiply(new BigDecimal("0.015"), RoundingMode.HALF_UP);
        assertEquals(Money.of(Asset.USD, "1.50"), fee);
    }

    @Test
    void divides() {
        assertEquals(Money.of(Asset.USD, "2.50"),
                Money.of(Asset.USD, "10.00").divide(new BigDecimal("4"), RoundingMode.HALF_EVEN));
    }

    @Test
    void refusesToDivideByZero() {
        assertThrows(ArithmeticException.class,
                () -> Money.of(Asset.USD, "10.00").divide(BigDecimal.ZERO, RoundingMode.HALF_EVEN));
    }

    @Test
    void negatesAndTakesMagnitude() {
        Money debit = Money.of(Asset.USD, "-7.25");
        assertEquals(Money.of(Asset.USD, "7.25"), debit.negate());
        assertEquals(Money.of(Asset.USD, "7.25"), debit.abs());
    }

    @Test
    void splitsWithoutLosingACent() {
        List<Money> shares = Money.of(Asset.USD, "10.00").allocate(3);
        assertEquals(List.of(Money.of(Asset.USD, "3.34"), Money.of(Asset.USD, "3.33"), Money.of(Asset.USD, "3.33")),
                shares);
        assertEquals(Money.of(Asset.USD, "10.00"), sum(shares));
    }

    @Test
    void splitsWithoutLosingASatoshi() {
        Money total = Money.ofMinorUnits(Asset.BTC, 100);
        List<Money> shares = total.allocate(7);
        assertEquals(7, shares.size());
        assertEquals(total, sum(shares));
    }

    @Test
    void splitsInProportion() {
        List<Money> shares = Money.of(Asset.USD, "0.05").allocate(1, 1, 1);
        assertEquals(List.of(Money.of(Asset.USD, "0.02"), Money.of(Asset.USD, "0.02"), Money.of(Asset.USD, "0.01")),
                shares);
    }

    @Test
    void skipsSharesWeightedZeroWhenHandingOutTheRemainder() {
        List<Money> shares = Money.of(Asset.USD, "0.05").allocate(0, 1, 1);
        assertEquals(List.of(Money.zero(Asset.USD), Money.of(Asset.USD, "0.03"), Money.of(Asset.USD, "0.02")), shares);
    }

    @Test
    void splitsANegativeAmountWithoutLosingACent() {
        List<Money> shares = Money.of(Asset.USD, "-10.00").allocate(3);
        assertEquals(List.of(Money.of(Asset.USD, "-3.34"), Money.of(Asset.USD, "-3.33"), Money.of(Asset.USD, "-3.33")),
                shares);
        assertEquals(Money.of(Asset.USD, "-10.00"), sum(shares));
    }

    @Test
    void refusesNonsensicalSplits() {
        Money ten = Money.of(Asset.USD, "10.00");
        assertThrows(IllegalArgumentException.class, () -> ten.allocate(0));
        assertThrows(IllegalArgumentException.class, () -> ten.allocate(1, -1));
        assertThrows(IllegalArgumentException.class, () -> ten.allocate(0, 0));
    }

    @Test
    void comparesAmountsOfTheSameAsset() {
        Money limit = Money.of(Asset.USD, "1000.00");
        assertTrue(Money.of(Asset.USD, "1000.01").isGreaterThan(limit));
        assertTrue(Money.of(Asset.USD, "999.99").isLessThan(limit));
        assertFalse(limit.isGreaterThan(limit));
    }

    @Test
    void refusesToCompareAcrossAssets() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(Asset.BTC, "1").isGreaterThan(Money.of(Asset.ETH, "1")));
    }

    @Test
    void treatsTrailingZeroesAsTheSameAmount() {
        assertEquals(Money.of(Asset.USD, "1.50"), Money.of(Asset.USD, "1.5"));
        assertEquals(Money.of(Asset.USD, "1.500").hashCode(), Money.of(Asset.USD, "1.5").hashCode());
    }

    @Test
    void isNotEqualAcrossAssets() {
        assertNotEquals(Money.of(Asset.USDT, "1"), Money.of(Asset.USDC, "1"));
    }

    @Test
    void readsAsPlainDecimal() {
        assertEquals("0.00000001 BTC", Money.ofMinorUnits(Asset.BTC, 1).toString());
        assertEquals("0 JPY", Money.zero(Asset.JPY).toString());
    }

    private static Money sum(List<Money> amounts) {
        return amounts.stream().reduce(Money::add).orElseThrow();
    }

}
