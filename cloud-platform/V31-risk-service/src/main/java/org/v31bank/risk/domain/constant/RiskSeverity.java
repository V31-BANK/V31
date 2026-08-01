package org.v31bank.risk.domain.constant;

/**
 * Classification carried by a riskRule.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum RiskSeverity {

    /**
     * Recorded, and looked at in aggregate.
     */
    LOW,

    /**
     * Raises a case for an analyst.
     */
    MEDIUM,

    /**
     * Holds the operation until somebody releases it.
     */
    HIGH,

    /**
     * Refuses the operation outright.
     */
    CRITICAL

}
