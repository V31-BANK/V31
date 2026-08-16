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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An amount of one {@link Asset}, held exactly.
 * <p>
 * Every amount is kept at exactly the asset's {@link Asset#scale() scale}, so a balance
 * can only ever be a whole number of satoshis, wei or cents. Amounts that do not land on
 * that granularity are refused rather than quietly rounded:
 * {@code Money.of(Asset.USD, new BigDecimal("1.005"))} throws, because rounding money
 * without being asked to is how a system loses a fraction of every transaction. Where
 * rounding is the intent, it is requested explicitly and the {@link RoundingMode} is part
 * of the call.
 * <p>
 * Amounts are signed. A ledger needs both directions — a debit and its matching credit, a
 * balance overdrawn against a credit line — so the rule that a customer balance cannot go
 * below zero belongs to the domain that owns that balance, not to this type.
 * <p>
 * There is no {@code double} anywhere in this class, and none should be let near one: 0.1
 * + 0.2 is not 0.3 in binary floating point, and a bank that computes balances that way
 * will not reconcile.
 * <p>
 * Instances are immutable; every operation returns a new one.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class Money implements Comparable<Money> {

	private final Asset asset;

	/**
	 * Always scaled to {@code asset.scale()}, which is what makes the unscaled value the
	 * amount in minor units and lets equality compare exactly.
	 */
	private final BigDecimal amount;

	private Money(Asset asset, BigDecimal amount) {
		this.asset = asset;
		this.amount = amount;
	}

	/**
	 * Create an amount, refusing any value the asset cannot represent exactly.
	 * @param asset the asset the amount is denominated in
	 * @param amount the amount, with no more decimal places than the asset has
	 * @return the amount
	 * @throws IllegalArgumentException if the value carries more precision than the asset
	 * allows
	 */
	public static Money of(Asset asset, BigDecimal amount) {
		Objects.requireNonNull(asset, "asset must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		try {
			return new Money(asset, amount.setScale(asset.scale(), RoundingMode.UNNECESSARY));
		}
		catch (ArithmeticException ex) {
			throw new IllegalArgumentException("%s cannot hold %s exactly: it divides into %d decimal places"
				.formatted(asset.code(), amount.toPlainString(), asset.scale()), ex);
		}
	}

	/**
	 * Create an amount from its decimal text, the form amounts arrive in over the wire
	 * and leave in towards a database.
	 * @param asset the asset the amount is denominated in
	 * @param amount the amount, for example {@code "0.00012345"}
	 * @return the amount
	 * @throws IllegalArgumentException if the text is not a number, or carries more
	 * precision than the asset allows
	 */
	public static Money of(Asset asset, String amount) {
		Objects.requireNonNull(amount, "amount must not be null");
		try {
			return of(asset, new BigDecimal(amount));
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("'" + amount + "' is not a decimal amount", ex);
		}
	}

	/**
	 * Create an amount, rounding to what the asset can represent.
	 * <p>
	 * Reserved for the places where rounding is the decision being made — pricing a
	 * quote, charging a fee — and the direction has been chosen on purpose. Rounding a
	 * customer's balance is not one of them.
	 * @param asset the asset the amount is denominated in
	 * @param amount the amount to round
	 * @param rounding how to round it
	 * @return the rounded amount
	 */
	public static Money of(Asset asset, BigDecimal amount, RoundingMode rounding) {
		Objects.requireNonNull(asset, "asset must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(rounding, "rounding must not be null");
		return new Money(asset, amount.setScale(asset.scale(), rounding));
	}

	/**
	 * Return nothing, denominated in the given asset.
	 * @param asset the asset the amount is denominated in
	 * @return zero
	 */
	public static Money zero(Asset asset) {
		Objects.requireNonNull(asset, "asset must not be null");
		return new Money(asset, BigDecimal.ZERO.setScale(asset.scale()));
	}

	/**
	 * Create an amount from a count of the asset's smallest units, the form blockchains
	 * and card networks use.
	 * @param asset the asset the amount is denominated in
	 * @param minorUnits the number of satoshis, cents, or whatever the asset divides into
	 * @return the amount
	 */
	public static Money ofMinorUnits(Asset asset, long minorUnits) {
		return ofMinorUnits(asset, BigInteger.valueOf(minorUnits));
	}

	/**
	 * Create an amount from a count of the asset's smallest units, taken as a
	 * {@link BigInteger} because a balance in wei does not fit in a {@code long}.
	 * @param asset the asset the amount is denominated in
	 * @param minorUnits the number of smallest units
	 * @return the amount
	 */
	public static Money ofMinorUnits(Asset asset, BigInteger minorUnits) {
		Objects.requireNonNull(asset, "asset must not be null");
		Objects.requireNonNull(minorUnits, "minorUnits must not be null");
		return new Money(asset, new BigDecimal(minorUnits, asset.scale()));
	}

	/**
	 * Add another amount of the same asset.
	 * @param other the amount to add
	 * @return the sum
	 * @throws IllegalArgumentException if the assets differ
	 */
	public Money add(Money other) {
		requireSameAsset(other, "add");
		return new Money(this.asset, this.amount.add(other.amount));
	}

	/**
	 * Subtract another amount of the same asset. The result may be negative.
	 * @param other the amount to subtract
	 * @return the difference
	 * @throws IllegalArgumentException if the assets differ
	 */
	public Money subtract(Money other) {
		requireSameAsset(other, "subtract");
		return new Money(this.asset, this.amount.subtract(other.amount));
	}

	/**
	 * Scale this amount by a factor — a fee rate, an interest rate — rounding the result
	 * to what the asset can represent.
	 * @param factor the factor to multiply by
	 * @param rounding how to round the result
	 * @return the scaled amount
	 */
	public Money multiply(BigDecimal factor, RoundingMode rounding) {
		Objects.requireNonNull(factor, "factor must not be null");
		Objects.requireNonNull(rounding, "rounding must not be null");
		return new Money(this.asset, this.amount.multiply(factor).setScale(this.asset.scale(), rounding));
	}

	/**
	 * Divide this amount, rounding the result to what the asset can represent.
	 * <p>
	 * Splitting an amount between recipients is {@link #allocate(int)} instead: dividing
	 * and rounding each share loses or invents the remainder, which {@code allocate}
	 * distributes rather than discarding.
	 * @param divisor the value to divide by
	 * @param rounding how to round the result
	 * @return the quotient
	 * @throws ArithmeticException if the divisor is zero
	 */
	public Money divide(BigDecimal divisor, RoundingMode rounding) {
		Objects.requireNonNull(divisor, "divisor must not be null");
		Objects.requireNonNull(rounding, "rounding must not be null");
		return new Money(this.asset, this.amount.divide(divisor, this.asset.scale(), rounding));
	}

	/**
	 * Return this amount with the opposite sign, which is how one side of a double entry
	 * is derived from the other.
	 * @return the negated amount
	 */
	public Money negate() {
		return new Money(this.asset, this.amount.negate());
	}

	/**
	 * Return the magnitude of this amount, discarding its direction.
	 * @return the absolute amount
	 */
	public Money abs() {
		return new Money(this.asset, this.amount.abs());
	}

	/**
	 * Split this amount into equal shares, giving the remainder away one minor unit at a
	 * time so that the shares add back up to the original exactly.
	 * <p>
	 * Ten dollars split three ways is 3.34, 3.33 and 3.33 — never three times 3.3333
	 * rounded, which reconciles to a cent short.
	 * @param parts the number of shares
	 * @return the shares, which sum to this amount
	 */
	public List<Money> allocate(int parts) {
		if (parts < 1) {
			throw new IllegalArgumentException("An amount is split into at least one share, not " + parts);
		}
		int[] weights = new int[parts];
		Arrays.fill(weights, 1);
		return allocate(weights);
	}

	/**
	 * Split this amount in the given proportions, giving the remainder to the earlier
	 * shares one minor unit at a time so that the shares add back up to the original
	 * exactly.
	 * @param weights the proportions, which must not be negative and must not all be zero
	 * @return the shares, in the order of the weights, which sum to this amount
	 */
	public List<Money> allocate(int... weights) {
		Objects.requireNonNull(weights, "weights must not be null");
		if (weights.length == 0) {
			throw new IllegalArgumentException("At least one weight is required");
		}
		long totalWeight = 0;
		for (int weight : weights) {
			if (weight < 0) {
				throw new IllegalArgumentException("A share cannot have a negative weight: " + weight);
			}
			totalWeight += weight;
		}
		if (totalWeight == 0) {
			throw new IllegalArgumentException("The weights must not all be zero");
		}
		BigInteger total = toMinorUnits();
		BigInteger divisor = BigInteger.valueOf(totalWeight);
		BigInteger[] shares = new BigInteger[weights.length];
		BigInteger distributed = BigInteger.ZERO;
		for (int i = 0; i < weights.length; i++) {
			shares[i] = total.multiply(BigInteger.valueOf(weights[i])).divide(divisor);
			distributed = distributed.add(shares[i]);
		}
		BigInteger remainder = total.subtract(distributed);
		BigInteger unit = BigInteger.valueOf(remainder.signum());
		for (int i = 0; remainder.signum() != 0; i = (i + 1) % weights.length) {
			if (weights[i] > 0) {
				shares[i] = shares[i].add(unit);
				remainder = remainder.subtract(unit);
			}
		}
		List<Money> allocation = new ArrayList<>(shares.length);
		for (BigInteger share : shares) {
			allocation.add(ofMinorUnits(this.asset, share));
		}
		return List.copyOf(allocation);
	}

	/**
	 * Return this amount as a count of the asset's smallest units — satoshis for bitcoin,
	 * wei for ether — which is the form a blockchain expects.
	 * @return the amount in minor units
	 */
	public BigInteger toMinorUnits() {
		return this.amount.unscaledValue();
	}

	public boolean isZero() {
		return this.amount.signum() == 0;
	}

	public boolean isPositive() {
		return this.amount.signum() > 0;
	}

	public boolean isNegative() {
		return this.amount.signum() < 0;
	}

	/**
	 * Whether this amount exceeds another of the same asset, for the limit and balance
	 * checks that read better than a comparison against zero.
	 * @param other the amount to compare against
	 * @return {@code true} if this amount is the greater
	 * @throws IllegalArgumentException if the assets differ
	 */
	public boolean isGreaterThan(Money other) {
		return compareTo(other) > 0;
	}

	/**
	 * Whether this amount falls short of another of the same asset.
	 * @param other the amount to compare against
	 * @return {@code true} if this amount is the lesser
	 * @throws IllegalArgumentException if the assets differ
	 */
	public boolean isLessThan(Money other) {
		return compareTo(other) < 0;
	}

	public Asset getAsset() {
		return this.asset;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	@Override
	public int compareTo(Money other) {
		requireSameAsset(other, "compare");
		return this.amount.compareTo(other.amount);
	}

	private void requireSameAsset(Money other, String operation) {
		Objects.requireNonNull(other, "other must not be null");
		if (!this.asset.equals(other.asset)) {
			throw new IllegalArgumentException("Cannot %s %s and %s: crossing assets needs an explicit %s"
				.formatted(operation, this.asset.code(), other.asset.code(), FxRate.class.getSimpleName()));
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Money other && this.asset.equals(other.asset) && this.amount.equals(other.amount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.asset, this.amount);
	}

	/**
	 * Return the amount and its asset, never in scientific notation, so that a log line
	 * reads {@code 0.00012345 BTC} rather than {@code 1.2345E-4 BTC}.
	 * @return the amount as text
	 */
	@Override
	public String toString() {
		return this.amount.toPlainString() + " " + this.asset.code();
	}

}
