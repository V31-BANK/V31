package org.v31bank.customer.domain.valueobject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;


/**
 * A Value Object is an object that combines a group of business-related
 * fields into a single meaningful concept.
 * <p>
 * The whole object is treated as a value, and any change to
 * its components results in a new value.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record Money(
        BigDecimal amount,
        Currency currency
) {

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null.");
        Objects.requireNonNull(currency, "Currency must not be null.");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(
                amount.add(other.amount),
                currency
        );
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(
                amount.subtract(other.amount),
                currency
        );
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) < 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "Money must not be null.");

        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s"
                            .formatted(currency, other.currency)
            );
        }
    }
}
