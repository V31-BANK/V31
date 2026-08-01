package org.v31bank.risk.application.port.in;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.risk.domain.constant.RiskSeverity;
import org.v31bank.risk.application.dto.RiskRulePageQuery;
import org.v31bank.risk.domain.constant.RiskRuleStatus;
import org.v31bank.risk.domain.model.RiskRule;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Use cases for managing risk rules.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface RiskRuleUseCase {

    /**
     * Add a risk rule, provided its code is not already taken.
     * @param code the code, unique
     * @param name the display name
     * @param severity the severity
     * @return the outcome of the command
     */
    ApiResponse<RiskRule> create(String code, String name, RiskSeverity severity);

    Optional<RiskRule> get(UUID id);

    /**
     * Find a page of risk rules matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    PageResult<RiskRule> page(RiskRulePageQuery query);

    /**
     * Update the risk rule with the given identifier.
     * @param id the record to update
     * @param code the code, which must not belong to another record
     * @param name the display name
     * @param severity the severity
     * @param status the new status, or {@code null} to leave it unchanged
     * @return the outcome of the command
     */
    ApiResponse<RiskRule> update(UUID id, String code, String name, RiskSeverity severity, RiskRuleStatus status);

    /**
     * Delete the risk rule with the given identifier.
     * @param id the record to delete
     * @return the outcome of the command
     */
    ApiResponse<RiskRule> delete(UUID id);

}
