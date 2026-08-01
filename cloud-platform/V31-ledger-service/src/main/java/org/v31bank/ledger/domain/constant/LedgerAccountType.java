package org.v31bank.ledger.domain.constant;

/**
 * Classification carried by a ledgerAccount.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum LedgerAccountType {

    /**
     * What the bank owns or is owed. Increases on the debit side.
     */
    ASSET,

    /**
     * What the bank owes, including every customer balance. Increases on the credit side.
     */
    LIABILITY,

    /**
     * What is left for the owners once liabilities are met.
     */
    EQUITY,

    /**
     * What the bank earns — fees, spreads, interest received.
     */
    REVENUE,

    /**
     * What the bank spends, including interest paid.
     */
    EXPENSE

}
