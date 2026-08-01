package org.v31bank.transfer.domain.constant;

/**
 * Lifecycle status of a transferLimit.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum TransferLimitStatus {

    /**
     * Enforced on every transfer it applies to.
     */
    ACTIVE,

    /**
     * Kept for the record but not enforced. A suspended limit is a deliberate act and should be short-lived.
     */
    SUSPENDED

}
