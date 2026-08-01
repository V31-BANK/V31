package org.v31bank.risk.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.v31bank.risk.domain.constant.RiskSeverity;
import org.v31bank.risk.application.dto.RiskRulePageQuery;
import org.v31bank.risk.application.port.in.RiskRuleUseCase;
import org.v31bank.risk.application.port.out.RiskRulePort;
import org.v31bank.risk.domain.constant.RiskRuleStatus;
import org.v31bank.risk.domain.model.RiskRule;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Default {@link RiskRuleUseCase} implementation.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
@Transactional
public class RiskRuleService implements RiskRuleUseCase {

    private final RiskRulePort riskRuleRepository;

    public RiskRuleService(RiskRulePort riskRuleRepository) {
        this.riskRuleRepository = riskRuleRepository;
    }

    @Override
    public ApiResponse<RiskRule> create(String code, String name, RiskSeverity severity) {
        if (this.riskRuleRepository.existsByCode(code)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Code '" + code + "' is already in use");
        }
        RiskRule riskRule = new RiskRule();
        riskRule.setCode(code);
        riskRule.setName(name);
        riskRule.setSeverity(severity);
        return ApiResponse.ok(this.riskRuleRepository.save(riskRule));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RiskRule> get(UUID id) {
        return this.riskRuleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RiskRule> page(RiskRulePageQuery query) {
        return this.riskRuleRepository.findPage(query);
    }

    @Override
    public ApiResponse<RiskRule> update(UUID id, String code, String name, RiskSeverity severity, RiskRuleStatus status) {
        Optional<RiskRule> found = this.riskRuleRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No risk rule exists with id " + id);
        }
        RiskRule riskRule = found.get();
        if (!riskRule.getCode().equals(code) && this.riskRuleRepository.existsByCode(code)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Code '" + code + "' is already in use");
        }
        riskRule.setCode(code);
        riskRule.setName(name);
        riskRule.setSeverity(severity);
        if (status != null) {
            riskRule.setStatus(status);
        }
        return ApiResponse.ok(this.riskRuleRepository.save(riskRule));
    }

    @Override
    public ApiResponse<RiskRule> delete(UUID id) {
        Optional<RiskRule> found = this.riskRuleRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No risk rule exists with id " + id);
        }
        RiskRule riskRule = found.get();
        this.riskRuleRepository.delete(riskRule);
        return ApiResponse.ok(riskRule);
    }

}
