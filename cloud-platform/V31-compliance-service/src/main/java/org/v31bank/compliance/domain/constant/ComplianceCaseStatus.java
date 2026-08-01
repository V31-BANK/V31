package org.v31bank.compliance.domain.constant;

/**
 * How far a compliance case has got.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum ComplianceCaseStatus {

    /**
     * Raised, and waiting for an analyst.
     */
    OPEN,

    /**
     * An analyst is working on it.
     */
    IN_REVIEW,

    /**
     * Referred upwards, typically to the money laundering reporting officer.
     */
    ESCALATED,

    /**
     * Concluded. A case is never deleted once closed — the decision and its
     * timing are what a regulator asks for.
     */
    CLOSED

}
