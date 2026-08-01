package org.v31bank.risk.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.risk.domain.constant.RiskSeverity;
import org.v31bank.risk.domain.constant.RiskRuleStatus;
import org.v31bank.risk.domain.model.RiskRule;

/**
 * API representation of a risk rule.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record RiskRuleResponse(UUID id, String code, String name, RiskSeverity severity, RiskRuleStatus status, Instant createdDate,
        Instant lastModifiedDate) {

    public static RiskRuleResponse from(RiskRule riskRule) {
        return new RiskRuleResponse(riskRule.getId(), riskRule.getCode(), riskRule.getName(), riskRule.getSeverity(), riskRule.getStatus(),
                riskRule.getCreatedDate(), riskRule.getLastModifiedDate());
    }

}
