package org.v31bank.cbs.domain.constant;

/**
 * What kind of account a bank product opens.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum BankProductCategory {

    /**
     * Balance held on demand, withdrawable at any time.
     */
    SAVINGS,

    /**
     * Balance used for day-to-day movement, with no interest.
     */
    CURRENT,

    /**
     * Balance committed for a fixed term in exchange for a higher rate.
     */
    TERM_DEPOSIT,

    /**
     * A line the customer draws on rather than a balance they hold.
     */
    CREDIT

}
