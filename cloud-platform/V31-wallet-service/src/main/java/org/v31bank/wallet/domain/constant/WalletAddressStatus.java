package org.v31bank.wallet.domain.constant;

/**
 * Lifecycle status of a walletAddress.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum WalletAddressStatus {

    /**
     * Registered, and waiting for a second pair of eyes before anything can be sent to it.
     */
    PENDING,

    /**
     * Approved. Withdrawals may name it.
     */
    ACTIVE,

    /**
     * Refused, or withdrawn after the fact. Nothing may be sent to it.
     */
    BLOCKED

}
