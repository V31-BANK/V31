package org.v31bank.risk.application.dto;

import org.v31bank.risk.domain.constant.RiskSeverity;
import org.v31bank.risk.domain.constant.RiskRuleStatus;
import org.v31bank.data.jpa.domain.PageQuery;

/**
 * Paginated risk rule query with optional filters.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class RiskRulePageQuery extends PageQuery {

    /**
     * Code fragment to match, case-insensitive.
     */
    private String code;

    /**
     * Severity to match.
     */
    private RiskSeverity severity;

    /**
     * Status to match.
     */
    private RiskRuleStatus status;

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public RiskSeverity getSeverity() {
        return this.severity;
    }

    public void setSeverity(RiskSeverity severity) {
        this.severity = severity;
    }

    public RiskRuleStatus getStatus() {
        return this.status;
    }

    public void setStatus(RiskRuleStatus status) {
        this.status = status;
    }

}
