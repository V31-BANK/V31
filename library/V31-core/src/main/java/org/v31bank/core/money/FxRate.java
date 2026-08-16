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
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * The price of one asset in another at a point in time — {@code 1 BTC = 64200.50
 * USD} — and the only sanctioned way to turn {@link Money} of one asset into
 * {@link Money} of another.
 * <p>
 * The rate is directional: it says how much {@link #quote()} one unit of {@link #base()}
 * buys, and {@link #convert(Money)} refuses anything not denominated in the base. Getting
 * that backwards silently is a class of bug that only shows up in the reconciliation,
 * which is why it fails loudly instead.
 * <p>
 * {@link #asOf()} is carried because a crypto rate is worth minutes at most. Pricing a
 * movement against a rate that has gone stale is a real loss, so the age is checked
 * before the rate is used rather than assumed.
 *
 * @param base the asset being priced
 * @param quote the asset it is priced in
 * @param rate how much of the quote asset one unit of the base buys, always greater than
 * zero
 * @param asOf when the rate was observed
 * @author Xander Wang
 * @since 0.2.0
 */
public record FxRate(Asset base, Asset quote, BigDecimal rate, Instant asOf) {

	/**
	 * The precision inversion works at, chosen to be far finer than any asset's own so
	 * that the inverted rate is limited by the input rather than by this calculation.
	 */
	private static final MathContext INVERSION_PRECISION = MathContext.DECIMAL128;

	public FxRate {
		Objects.requireNonNull(base, "base must not be null");
		Objects.requireNonNull(quote, "quote must not be null");
		Objects.requireNonNull(rate, "rate must not be null");
		Objects.requireNonNull(asOf, "asOf must not be null");
		if (base.equals(quote)) {
			throw new IllegalArgumentException("A rate prices one asset in another, but both sides are " + base.code());
		}
		if (rate.signum() <= 0) {
			throw new IllegalArgumentException("A %s/%s rate must be greater than zero, not %s".formatted(base.code(),
					quote.code(), rate.toPlainString()));
		}
	}

	/**
	 * Convert an amount of the base asset into the quote asset, rounding to what the
	 * quote asset can represent using banker's rounding, which does not drift in the
	 * bank's favour or the customer's over many conversions.
	 * @param amount the amount to convert, denominated in the base asset
	 * @return the converted amount, denominated in the quote asset
	 * @throws IllegalArgumentException if the amount is not in the base asset
	 */
	public Money convert(Money amount) {
		return convert(amount, RoundingMode.HALF_EVEN);
	}

	/**
	 * Convert an amount of the base asset into the quote asset, rounding as asked — a
	 * quote given to a customer is usually rounded against the party that requested it
	 * rather than neutrally.
	 * @param amount the amount to convert, denominated in the base asset
	 * @param rounding how to round the result
	 * @return the converted amount, denominated in the quote asset
	 * @throws IllegalArgumentException if the amount is not in the base asset
	 */
	public Money convert(Money amount, RoundingMode rounding) {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(rounding, "rounding must not be null");
		if (!this.base.equals(amount.getAsset())) {
			throw new IllegalArgumentException("A %s/%s rate converts %s, not %s".formatted(this.base.code(),
					this.quote.code(), this.base.code(), amount.getAsset().code()));
		}
		return Money.of(this.quote, amount.getAmount().multiply(this.rate), rounding);
	}

	/**
	 * Return the rate the other way round.
	 * <p>
	 * Inversion divides, so the result is only as good as the precision it is computed at
	 * and will not always round-trip back to this rate exactly. Where both directions are
	 * quoted — which is normally the case, since a market spreads them apart rather than
	 * mirroring them — take the quoted one instead.
	 * @return the inverted rate, observed at the same instant
	 */
	public FxRate inverse() {
		return new FxRate(this.quote, this.base, BigDecimal.ONE.divide(this.rate, INVERSION_PRECISION), this.asOf);
	}

	/**
	 * Whether this rate was observed longer ago than the caller is willing to accept.
	 * @param maxAge the oldest the rate may be
	 * @param now the instant to measure against
	 * @return {@code true} if the rate is too old to price against
	 */
	public boolean isOlderThan(Duration maxAge, Instant now) {
		Objects.requireNonNull(maxAge, "maxAge must not be null");
		Objects.requireNonNull(now, "now must not be null");
		return Duration.between(this.asOf, now).compareTo(maxAge) > 0;
	}

	@Override
	public String toString() {
		return "1 %s = %s %s".formatted(this.base.code(), this.rate.toPlainString(), this.quote.code());
	}

}
