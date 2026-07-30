package org.v31bank.customer.domain.valueobject;

import java.math.BigDecimal;
import java.util.Currency;


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
public final class Money {

    private final BigDecimal amount;
    private final Currency currency;


    public Money(BigDecimal amount, Currency currency) {

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        this.amount = amount;
        this.currency = currency;
    }


    public Money add(Money other) {

        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch"
            );
        }

        return new Money(
                this.amount.add(other.amount),
                this.currency
        );
    }


    public Money subtract(Money other) {

        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch"
            );
        }

        return new Money(
                this.amount.subtract(other.amount),
                this.currency
        );
    }


    public BigDecimal getAmount() {
        return amount;
    }


    public Currency getCurrency() {
        return currency;
    }
}
