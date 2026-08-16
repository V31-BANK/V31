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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Something the bank can hold a balance of, whether it is a currency, a native blockchain
 * asset, or a token.
 * <p>
 * This exists because {@link java.util.Currency} cannot express what this bank holds: it
 * is limited to ISO 4217, so it has no BTC and no ETH, and its notion of precision stops
 * well short of what a native asset needs.
 * <p>
 * The {@link #scale()} is the number of decimal places the asset divides into — 2 for
 * USD, 8 for BTC, 18 for ETH — and it is the whole reason this type is carried around
 * rather than a bare currency code: it decides what {@link Money} is allowed to
 * represent, and rounding to the wrong one either invents value or destroys it.
 * <p>
 * The scale is part of the identity, deliberately. The same ticker can be issued with
 * different precision on different chains, and comparing only the code would let two
 * balances a factor of a trillion apart look like the same asset.
 * <p>
 * The constants below are the assets this library knows by name. A token it does not list
 * is constructed directly — {@code new Asset("PEPE", AssetType.CRYPTO,
 * 18)} — since the tradeable list belongs in a service's configuration, not in a shared
 * library that would need releasing every time one is added.
 *
 * @param code the ticker, upper case, for example {@code BTC} or {@code USD}
 * @param type what kind of asset it is
 * @param scale the number of decimal places it divides into
 * @author Xander Wang
 * @since 0.2.0
 */
public record Asset(String code, AssetType type, int scale) implements Comparable<Asset> {

	/**
	 * The largest precision an asset may declare. Eighteen covers every asset in common
	 * use; the headroom above it is for tokens that chose more.
	 */
	public static final int MAX_SCALE = 30;

	private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9]{2,16}");

	/**
	 * United States dollar.
	 */
	public static final Asset USD = new Asset("USD", AssetType.FIAT, 2);

	/**
	 * Euro.
	 */
	public static final Asset EUR = new Asset("EUR", AssetType.FIAT, 2);

	/**
	 * Pound sterling.
	 */
	public static final Asset GBP = new Asset("GBP", AssetType.FIAT, 2);

	/**
	 * Swiss franc.
	 */
	public static final Asset CHF = new Asset("CHF", AssetType.FIAT, 2);

	/**
	 * Singapore dollar.
	 */
	public static final Asset SGD = new Asset("SGD", AssetType.FIAT, 2);

	/**
	 * Japanese yen, which has no minor unit: an amount of yen is a whole number.
	 */
	public static final Asset JPY = new Asset("JPY", AssetType.FIAT, 0);

	/**
	 * Bitcoin. Its minor unit is the satoshi, one hundred millionth of a bitcoin.
	 */
	public static final Asset BTC = new Asset("BTC", AssetType.CRYPTO, 8);

	/**
	 * Ether. Its minor unit is the wei, one quintillionth of an ether, which is why an
	 * ether balance does not fit in a {@code long} and is held as a
	 * {@link java.math.BigInteger} in minor units.
	 */
	public static final Asset ETH = new Asset("ETH", AssetType.CRYPTO, 18);

	/**
	 * Solana. Its minor unit is the lamport.
	 */
	public static final Asset SOL = new Asset("SOL", AssetType.CRYPTO, 9);

	/**
	 * XRP. Its minor unit is the drop.
	 */
	public static final Asset XRP = new Asset("XRP", AssetType.CRYPTO, 6);

	/**
	 * Tether, as issued on the chains that give it six decimal places.
	 */
	public static final Asset USDT = new Asset("USDT", AssetType.STABLECOIN, 6);

	/**
	 * USD Coin, as issued on the chains that give it six decimal places.
	 */
	public static final Asset USDC = new Asset("USDC", AssetType.STABLECOIN, 6);

	private static final Map<String, Asset> KNOWN = Stream
		.of(USD, EUR, GBP, CHF, SGD, JPY, BTC, ETH, SOL, XRP, USDT, USDC)
		.collect(Collectors.toUnmodifiableMap(Asset::code, Function.identity()));

	public Asset {
		Objects.requireNonNull(code, "code must not be null");
		Objects.requireNonNull(type, "type must not be null");
		code = code.trim().toUpperCase(Locale.ROOT);
		if (!CODE_PATTERN.matcher(code).matches()) {
			throw new IllegalArgumentException(
					"Asset code '" + code + "' must be 2 to 16 upper case letters or digits");
		}
		if (scale < 0 || scale > MAX_SCALE) {
			throw new IllegalArgumentException(
					"Asset " + code + " declares a scale of " + scale + ", outside 0 to " + MAX_SCALE);
		}
	}

	/**
	 * Return the asset this library knows by the given code, matched case insensitively.
	 * @param code the ticker to look up
	 * @return the asset
	 * @throws IllegalArgumentException if no asset is known by that code
	 */
	public static Asset of(String code) {
		return find(code).orElseThrow(() -> new IllegalArgumentException("No asset is known by the code '" + code
				+ "'; construct it directly to use one this library does not list"));
	}

	/**
	 * Look up an asset by code without failing when it is not known, for parsing input
	 * that may name anything.
	 * @param code the ticker to look up, which may be {@code null}
	 * @return the asset, or empty when none is known by that code
	 */
	public static Optional<Asset> find(String code) {
		if (code == null || code.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(KNOWN.get(code.trim().toUpperCase(Locale.ROOT)));
	}

	/**
	 * Return every asset this library knows by name.
	 * @return the known assets
	 */
	public static Set<Asset> known() {
		return Set.copyOf(KNOWN.values());
	}

	/**
	 * Whether this asset settles on a blockchain, and so cannot be recalled once a
	 * movement is broadcast.
	 * @return {@code true} for crypto and stablecoins
	 */
	public boolean isOnChain() {
		return this.type.isOnChain();
	}

	/**
	 * Return the smallest amount of this asset that can exist — one satoshi, one cent —
	 * which is the granularity every balance and every split has to land on.
	 * @return the minor unit as a fraction of one whole asset
	 */
	public BigDecimal minorUnit() {
		return BigDecimal.ONE.movePointLeft(this.scale);
	}

	@Override
	public int compareTo(Asset other) {
		return this.code.compareTo(other.code);
	}

	@Override
	public String toString() {
		return this.code;
	}

}
