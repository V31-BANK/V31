package org.v31bank.risk.presentation.dto;

import org.v31bank.risk.domain.constant.RiskSeverity;
import org.v31bank.risk.domain.constant.RiskRuleStatus;

/**
 * Request body for creating or updating a risk rule.
 *
 * @param code the code, unique
 * @param name the display name
 * @param severity the severity
 * @param status the status; ignored on create, where a record always starts
 * {@code DRAFT}, and left unchanged on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record RiskRuleRequest(String code, String name, RiskSeverity severity, RiskRuleStatus status) {

}
