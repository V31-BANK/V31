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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link Asset}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class AssetTests {

	@Test
	void knowsWhatEachAssetDividesInto() {
		assertThat(Asset.USD.scale()).isEqualTo(2);
		assertThat(Asset.JPY.scale()).isZero();
		assertThat(Asset.BTC.scale()).isEqualTo(8);
		assertThat(Asset.ETH.scale()).isEqualTo(18);
		assertThat(Asset.USDC.scale()).isEqualTo(6);
	}

	@Test
	void separatesOnChainAssetsFromFiat() {
		assertThat(Asset.USD.isOnChain()).isFalse();
		assertThat(Asset.BTC.isOnChain()).isTrue();
		assertThat(Asset.USDC.isOnChain()).isTrue();
		assertThat(Asset.USDC.type()).isEqualTo(AssetType.STABLECOIN);
	}

	@Test
	void normalisesTheCode() {
		assertThat(new Asset(" btc ", AssetType.CRYPTO, 8)).isEqualTo(Asset.BTC);
		assertThat(Asset.of("btc")).isEqualTo(Asset.BTC);
	}

	@Test
	void rejectsCodesThatAreNotTickers() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new Asset("B", AssetType.CRYPTO, 8));
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> new Asset("BTC-USD", AssetType.CRYPTO, 8));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new Asset("", AssetType.CRYPTO, 8));
	}

	@Test
	void rejectsImpossiblePrecision() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> new Asset("XYZ", AssetType.CRYPTO, -1));
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> new Asset("XYZ", AssetType.CRYPTO, Asset.MAX_SCALE + 1));
	}

	@Test
	void allowsATokenItDoesNotList() {
		Asset token = new Asset("PEPE", AssetType.CRYPTO, 18);
		assertThat(token.code()).isEqualTo("PEPE");
		assertThat(Asset.known()).doesNotContain(token);
	}

	@Test
	void refusesToGuessAtAnUnknownCode() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Asset.of("PEPE"))
			.withMessageContaining("No asset is known by the code 'PEPE'");
		assertThat(Asset.find("PEPE")).isEmpty();
		assertThat(Asset.find(null)).isEmpty();
	}

	@Test
	void treatsPrecisionAsPartOfIdentity() {
		assertThat(new Asset("USDC", AssetType.STABLECOIN, 18)).isNotEqualTo(Asset.USDC);
	}

	@Test
	void reportsItsSmallestUnit() {
		assertThat(Asset.BTC.minorUnit()).isEqualTo(new BigDecimal("0.00000001"));
		assertThat(Asset.JPY.minorUnit()).isEqualTo(BigDecimal.ONE);
	}

	@Test
	void readsAsItsTicker() {
		assertThat(Asset.BTC).hasToString("BTC");
	}

}
