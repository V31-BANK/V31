package org.v31bank.risk.domain.constant;

/**
 * Lifecycle status of a riskRule.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum RiskRuleStatus {

    /**
     * Being written, and not evaluated against anything.
     */
    DRAFT,

    /**
     * Evaluated on every operation it applies to.
     */
    ACTIVE,

    /**
     * Turned off, and kept so that past decisions can still be explained.
     */
    DISABLED

}
