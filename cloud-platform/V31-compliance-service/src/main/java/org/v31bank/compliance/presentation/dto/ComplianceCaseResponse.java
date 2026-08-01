package org.v31bank.compliance.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;
import org.v31bank.compliance.domain.model.ComplianceCase;

/**
 * API representation of a compliance case.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record ComplianceCaseResponse(UUID id, String caseNumber, UUID customerId, ComplianceCaseType type,
        ComplianceCaseStatus status, String summary, Instant createdDate, Instant lastModifiedDate) {

    public static ComplianceCaseResponse from(ComplianceCase complianceCase) {
        return new ComplianceCaseResponse(complianceCase.getId(), complianceCase.getCaseNumber(),
                complianceCase.getCustomerId(), complianceCase.getType(), complianceCase.getStatus(),
                complianceCase.getSummary(), complianceCase.getCreatedDate(), complianceCase.getLastModifiedDate());
    }

}
