package org.v31bank.compliance.presentation.dto;

import java.util.UUID;

import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;

/**
 * Request body for opening or updating a compliance case.
 *
 * @param caseNumber the reference the case is known by, unique
 * @param customerId the customer under investigation
 * @param type what is being investigated
 * @param status the status; ignored on create, where a case always starts
 * {@code OPEN}, and left unchanged on update when {@code null}
 * @param summary why the case was opened
 * @author Xander Wang
 * @since 0.2.0
 */
public record ComplianceCaseRequest(String caseNumber, UUID customerId, ComplianceCaseType type,
        ComplianceCaseStatus status, String summary) {

}
