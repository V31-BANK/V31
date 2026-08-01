package org.v31bank.cbs.domain.constant;

/**
 * Whether a bank product can still be opened.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum BankProductStatus {

    /**
     * Being prepared, and not offered to customers yet.
     */
    DRAFT,

    /**
     * On sale: new accounts can be opened against it.
     */
    ACTIVE,

    /**
     * Withdrawn from sale. Accounts already opened against it carry on, which is
     * why a withdrawn product is kept rather than deleted.
     */
    WITHDRAWN

}
