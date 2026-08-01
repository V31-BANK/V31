package org.v31bank.ledger.domain.constant;

/**
 * Lifecycle status of a ledgerAccount.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum LedgerAccountStatus {

    /**
     * Open for posting.
     */
    ACTIVE,

    /**
     * No longer posted to. Kept, because the postings that already reference it do not go away.
     */
    CLOSED

}
