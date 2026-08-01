package org.v31bank.compliance.domain.constant;

/**
 * What a compliance case is investigating.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum ComplianceCaseType {

    /**
     * Identity and documentation could not be verified automatically.
     */
    KYC,

    /**
     * Activity matched a money laundering pattern.
     */
    AML,

    /**
     * A party matched a sanctions or watchlist entry.
     */
    SANCTIONS,

    /**
     * Activity suggests the account is not under its owner's control.
     */
    FRAUD

}
