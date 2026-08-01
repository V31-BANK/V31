package org.v31bank.compliance.application.port.in;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.compliance.application.dto.ComplianceCasePageQuery;
import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;
import org.v31bank.compliance.domain.model.ComplianceCase;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.PageResponse;

/**
 * Use cases for managing compliance cases.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface ComplianceCaseUseCase {

    /**
     * Open a case, provided its number is not already taken.
     * @param caseNumber the reference the case is known by, unique
     * @param customerId the customer under investigation
     * @param type what is being investigated
     * @param summary why the case was opened
     * @return the outcome of the command
     */
    ApiResponse<ComplianceCase> create(String caseNumber, UUID customerId, ComplianceCaseType type, String summary);

    Optional<ComplianceCase> get(UUID id);

    /**
     * Find a page of cases matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching cases
     */
    PageResponse<ComplianceCase> page(ComplianceCasePageQuery query);

    /**
     * Update the case with the given identifier.
     * @param id the case to update
     * @param caseNumber the reference, which must not belong to another case
     * @param customerId the customer under investigation
     * @param type what is being investigated
     * @param status the new status, or {@code null} to leave it unchanged
     * @param summary why the case was opened
     * @return the outcome of the command
     */
    ApiResponse<ComplianceCase> update(UUID id, String caseNumber, UUID customerId, ComplianceCaseType type,
            ComplianceCaseStatus status, String summary);

    /**
     * Delete the case with the given identifier.
     * @param id the case to delete
     * @return the outcome of the command
     */
    ApiResponse<ComplianceCase> delete(UUID id);

}
