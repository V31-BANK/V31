package org.v31bank.compliance.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.v31bank.compliance.application.dto.ComplianceCasePageQuery;
import org.v31bank.compliance.application.port.in.ComplianceCaseUseCase;
import org.v31bank.compliance.application.port.out.ComplianceCasePort;
import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;
import org.v31bank.compliance.domain.model.ComplianceCase;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.PageResponse;

/**
 * Default {@link ComplianceCaseUseCase} implementation.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
@Transactional
public class ComplianceCaseService implements ComplianceCaseUseCase {

    private final ComplianceCasePort complianceCaseRepository;

    public ComplianceCaseService(ComplianceCasePort complianceCaseRepository) {
        this.complianceCaseRepository = complianceCaseRepository;
    }

    @Override
    public ApiResponse<ComplianceCase> create(String caseNumber, UUID customerId, ComplianceCaseType type,
            String summary) {
        if (this.complianceCaseRepository.existsByCaseNumber(caseNumber)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT,
                    "Compliance case number '" + caseNumber + "' is already in use");
        }
        ComplianceCase complianceCase = new ComplianceCase();
        complianceCase.setCaseNumber(caseNumber);
        complianceCase.setCustomerId(customerId);
        complianceCase.setType(type);
        complianceCase.setSummary(summary);
        return ApiResponse.ok(this.complianceCaseRepository.save(complianceCase));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplianceCase> get(UUID id) {
        return this.complianceCaseRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ComplianceCase> page(ComplianceCasePageQuery query) {
        return this.complianceCaseRepository.findPage(query);
    }

    @Override
    public ApiResponse<ComplianceCase> update(UUID id, String caseNumber, UUID customerId, ComplianceCaseType type,
            ComplianceCaseStatus status, String summary) {
        Optional<ComplianceCase> found = this.complianceCaseRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No compliance case exists with id " + id);
        }
        ComplianceCase complianceCase = found.get();
        if (!complianceCase.getCaseNumber().equals(caseNumber)
                && this.complianceCaseRepository.existsByCaseNumber(caseNumber)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT,
                    "Compliance case number '" + caseNumber + "' is already in use");
        }
        if (complianceCase.getStatus() == ComplianceCaseStatus.CLOSED) {
            return ApiResponse.error(CommonErrorCode.CONFLICT,
                    "Compliance case " + id + " is closed and cannot be changed");
        }
        complianceCase.setCaseNumber(caseNumber);
        complianceCase.setCustomerId(customerId);
        complianceCase.setType(type);
        complianceCase.setSummary(summary);
        if (status != null) {
            complianceCase.setStatus(status);
        }
        return ApiResponse.ok(this.complianceCaseRepository.save(complianceCase));
    }

    /**
     * Delete a case that has not been concluded.
     * <p>
     * A closed case is kept: it records a decision that was reached and when, and
     * a regulator asking why an account was frozen expects to find it. Only a case
     * still in progress — raised in error, a duplicate of another — can be removed.
     */
    @Override
    public ApiResponse<ComplianceCase> delete(UUID id) {
        Optional<ComplianceCase> found = this.complianceCaseRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No compliance case exists with id " + id);
        }
        ComplianceCase complianceCase = found.get();
        if (complianceCase.getStatus() == ComplianceCaseStatus.CLOSED) {
            return ApiResponse.error(CommonErrorCode.CONFLICT,
                    "Compliance case " + id + " is closed and is kept as a record of the decision");
        }
        this.complianceCaseRepository.deleteById(id);
        return ApiResponse.ok(complianceCase);
    }

}
