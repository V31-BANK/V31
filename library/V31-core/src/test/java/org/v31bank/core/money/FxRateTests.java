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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
		assertThat(rate.convert(Money.of(Asset.BTC, "0.5"))).isEqualTo(Money.of(Asset.USD, "30000.00"));
	}

	@Test
	void roundsNeutrallyByDefault() {
		FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("2.005"), OBSERVED_AT);
		assertThat(rate.convert(Money.of(Asset.BTC, "1"))).isEqualTo(Money.of(Asset.USD, "2.00"));
	}

	@Test
	void roundsAsAsked() {
		FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("2.005"), OBSERVED_AT);
		assertThat(rate.convert(Money.of(Asset.BTC, "1"), RoundingMode.UP)).isEqualTo(Money.of(Asset.USD, "2.01"));
	}

	@Test
	void refusesToConvertTheWrongWayRound() {
		FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("60000.00"), OBSERVED_AT);
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> rate.convert(Money.of(Asset.USD, "100.00")))
			.withMessageContaining("A BTC/USD rate converts BTC, not USD");
	}

	@Test
	void refusesARateAgainstItself() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> new FxRate(Asset.USD, Asset.USD, BigDecimal.ONE, OBSERVED_AT));
	}

	@Test
	void refusesARateThatIsNotPositive() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> new FxRate(Asset.BTC, Asset.USD, BigDecimal.ZERO, OBSERVED_AT));
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> new FxRate(Asset.BTC, Asset.USD, new BigDecimal("-1"), OBSERVED_AT));
	}

	@Test
	void invertsTheDirection() {
		FxRate inverse = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("50000"), OBSERVED_AT).inverse();
		assertThat(inverse.base()).isEqualTo(Asset.USD);
		assertThat(inverse.quote()).isEqualTo(Asset.BTC);
		assertThat(inverse.convert(Money.of(Asset.USD, "50000.00"))).isEqualTo(Money.of(Asset.BTC, "1"));
	}

	@Test
	void reportsWhenItHasGoneStale() {
		FxRate rate = new FxRate(Asset.BTC, Asset.USD, new BigDecimal("60000.00"), OBSERVED_AT);
		assertThat(rate.isOlderThan(Duration.ofMinutes(1), OBSERVED_AT.plusSeconds(30))).isFalse();
		assertThat(rate.isOlderThan(Duration.ofMinutes(1), OBSERVED_AT.plusSeconds(90))).isTrue();
	}

	@Test
	void readsAsAQuote() {
		assertThat(new FxRate(Asset.BTC, Asset.USD, new BigDecimal("60000.00"), OBSERVED_AT))
			.hasToString("1 BTC = 60000.00 USD");
	}

}
