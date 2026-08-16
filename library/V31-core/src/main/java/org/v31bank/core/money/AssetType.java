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

/**
 * What kind of thing an {@link Asset} is, which is what most platform-wide rules branch
 * on: settlement finality, custody, and the compliance checks a movement has to pass all
 * differ between a bank transfer and an on-chain one.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum AssetType {

	/**
	 * State-issued currency held with a bank, identified by its ISO 4217 code. Settlement
	 * is reversible and runs on banking hours.
	 */
	FIAT,

	/**
	 * A blockchain's native asset, such as BTC or ETH. Settlement is final once confirmed
	 * and cannot be recalled, which is why an outgoing movement is checked before it is
	 * broadcast rather than after.
	 */
	CRYPTO,

	/**
	 * An on-chain token pegged to a fiat currency, such as USDC. It settles like
	 * {@link #CRYPTO} but carries the issuer's risk as well — an issuer can freeze a
	 * balance, which a native asset's holder never faces.
	 */
	STABLECOIN;

	/**
	 * Whether movements of this kind of asset settle on a blockchain, and are therefore
	 * irreversible once broadcast.
	 * @return {@code true} for everything but {@link #FIAT}
	 */
	public boolean isOnChain() {
		return this != FIAT;
	}

}
