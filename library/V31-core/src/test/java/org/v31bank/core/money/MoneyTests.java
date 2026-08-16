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

package org.v31bank.core.money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link Money}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class MoneyTests {

	@Test
	void refusesPrecisionTheAssetCannotHold() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Money.of(Asset.USD, new BigDecimal("1.005")))
			.withMessageContaining("USD cannot hold 1.005 exactly");
	}

	@Test
	void refusesASatoshiTooFar() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Money.of(Asset.BTC, "0.000000001"));
	}

	@Test
	void padsToTheAssetsScale() {
		assertThat(Money.of(Asset.USD, "1.5").getAmount()).isEqualTo(new BigDecimal("1.50"));
		assertThat(Money.of(Asset.BTC, "1").getAmount()).isEqualTo(new BigDecimal("1.00000000"));
	}

	@Test
	void roundsOnlyWhenAskedTo() {
		Money rounded = Money.of(Asset.USD, new BigDecimal("1.005"), RoundingMode.HALF_EVEN);
		assertThat(rounded.getAmount()).isEqualTo(new BigDecimal("1.00"));
	}

	@Test
	void rejectsTextThatIsNotAnAmount() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Money.of(Asset.USD, "not money"));
	}

	@Test
	void holdsAnEtherBalanceInWei() {
		Money ether = Money.of(Asset.ETH, "1");
		assertThat(ether.toMinorUnits()).isEqualTo(new BigInteger("1000000000000000000"));
		assertThat(Money.ofMinorUnits(Asset.ETH, new BigInteger("1000000000000000000"))).isEqualTo(ether);
	}

	@Test
	void holdsASingleSatoshi() {
		Money satoshi = Money.ofMinorUnits(Asset.BTC, 1);
		assertThat(satoshi.getAmount()).isEqualTo(new BigDecimal("0.00000001"));
		assertThat(satoshi.toMinorUnits()).isEqualTo(BigInteger.ONE);
	}

	@Test
	void adds() {
		assertThat(Money.of(Asset.USD, "1.25").add(Money.of(Asset.USD, "2.50"))).isEqualTo(Money.of(Asset.USD, "3.75"));
	}

	@Test
	void addsWithoutBinaryFloatingPointError() {
		assertThat(Money.of(Asset.USD, "0.10").add(Money.of(Asset.USD, "0.20"))).isEqualTo(Money.of(Asset.USD, "0.30"));
	}

	@Test
	void subtractsPastZero() {
		Money owed = Money.of(Asset.USD, "1.00").subtract(Money.of(Asset.USD, "2.50"));
		assertThat(owed).isEqualTo(Money.of(Asset.USD, "-1.50"));
		assertThat(owed.isNegative()).isTrue();
	}

	@Test
	void refusesToMixAssets() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Money.of(Asset.BTC, "1").add(Money.of(Asset.USD, "1")))
			.withMessageContaining("crossing assets needs an explicit FxRate");
	}

	@Test
	void multipliesAFee() {
		Money fee = Money.of(Asset.USD, "100.00").multiply(new BigDecimal("0.015"), RoundingMode.HALF_UP);
		assertThat(fee).isEqualTo(Money.of(Asset.USD, "1.50"));
	}

	@Test
	void divides() {
		assertThat(Money.of(Asset.USD, "10.00").divide(new BigDecimal("4"), RoundingMode.HALF_EVEN))
			.isEqualTo(Money.of(Asset.USD, "2.50"));
	}

	@Test
	void refusesToDivideByZero() {
		assertThatExceptionOfType(ArithmeticException.class)
			.isThrownBy(() -> Money.of(Asset.USD, "10.00").divide(BigDecimal.ZERO, RoundingMode.HALF_EVEN));
	}

	@Test
	void negatesAndTakesMagnitude() {
		Money debit = Money.of(Asset.USD, "-7.25");
		assertThat(debit.negate()).isEqualTo(Money.of(Asset.USD, "7.25"));
		assertThat(debit.abs()).isEqualTo(Money.of(Asset.USD, "7.25"));
	}

	@Test
	void splitsWithoutLosingACent() {
		List<Money> shares = Money.of(Asset.USD, "10.00").allocate(3);
		assertThat(shares).containsExactly(Money.of(Asset.USD, "3.34"), Money.of(Asset.USD, "3.33"),
				Money.of(Asset.USD, "3.33"));
		assertThat(sum(shares)).isEqualTo(Money.of(Asset.USD, "10.00"));
	}

	@Test
	void splitsWithoutLosingASatoshi() {
		Money total = Money.ofMinorUnits(Asset.BTC, 100);
		List<Money> shares = total.allocate(7);
		assertThat(shares).hasSize(7);
		assertThat(sum(shares)).isEqualTo(total);
	}

	@Test
	void splitsInProportion() {
		List<Money> shares = Money.of(Asset.USD, "0.05").allocate(1, 1, 1);
		assertThat(shares).containsExactly(Money.of(Asset.USD, "0.02"), Money.of(Asset.USD, "0.02"),
				Money.of(Asset.USD, "0.01"));
	}

	@Test
	void skipsSharesWeightedZeroWhenHandingOutTheRemainder() {
		List<Money> shares = Money.of(Asset.USD, "0.05").allocate(0, 1, 1);
		assertThat(shares).containsExactly(Money.zero(Asset.USD), Money.of(Asset.USD, "0.03"),
				Money.of(Asset.USD, "0.02"));
	}

	@Test
	void splitsANegativeAmountWithoutLosingACent() {
		List<Money> shares = Money.of(Asset.USD, "-10.00").allocate(3);
		assertThat(shares).containsExactly(Money.of(Asset.USD, "-3.34"), Money.of(Asset.USD, "-3.33"),
				Money.of(Asset.USD, "-3.33"));
		assertThat(sum(shares)).isEqualTo(Money.of(Asset.USD, "-10.00"));
	}

	@Test
	void refusesNonsensicalSplits() {
		Money ten = Money.of(Asset.USD, "10.00");
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ten.allocate(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ten.allocate(1, -1));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ten.allocate(0, 0));
	}

	@Test
	void comparesAmountsOfTheSameAsset() {
		Money limit = Money.of(Asset.USD, "1000.00");
		assertThat(Money.of(Asset.USD, "1000.01").isGreaterThan(limit)).isTrue();
		assertThat(Money.of(Asset.USD, "999.99").isLessThan(limit)).isTrue();
		assertThat(limit.isGreaterThan(limit)).isFalse();
	}

	@Test
	void refusesToCompareAcrossAssets() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Money.of(Asset.BTC, "1").isGreaterThan(Money.of(Asset.ETH, "1")));
	}

	@Test
	void treatsTrailingZeroesAsTheSameAmount() {
		assertThat(Money.of(Asset.USD, "1.5")).isEqualTo(Money.of(Asset.USD, "1.50"));
		assertThat(Money.of(Asset.USD, "1.5").hashCode()).isEqualTo(Money.of(Asset.USD, "1.500").hashCode());
	}

	@Test
	void isNotEqualAcrossAssets() {
		assertThat(Money.of(Asset.USDC, "1")).isNotEqualTo(Money.of(Asset.USDT, "1"));
	}

	@Test
	void readsAsPlainDecimal() {
		assertThat(Money.ofMinorUnits(Asset.BTC, 1)).hasToString("0.00000001 BTC");
		assertThat(Money.zero(Asset.JPY)).hasToString("0 JPY");
	}

	private static Money sum(List<Money> amounts) {
		return amounts.stream().reduce(Money::add).orElseThrow();
	}

}
