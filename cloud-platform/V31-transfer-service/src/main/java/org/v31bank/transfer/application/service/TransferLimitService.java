package org.v31bank.transfer.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.v31bank.transfer.application.dto.TransferLimitPageQuery;
import org.v31bank.transfer.application.port.in.TransferLimitUseCase;
import org.v31bank.transfer.application.port.out.TransferLimitPort;
import org.v31bank.transfer.domain.constant.TransferLimitStatus;
import org.v31bank.transfer.domain.model.TransferLimit;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Default {@link TransferLimitUseCase} implementation.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
@Transactional
public class TransferLimitService implements TransferLimitUseCase {

    private final TransferLimitPort transferLimitRepository;

    public TransferLimitService(TransferLimitPort transferLimitRepository) {
        this.transferLimitRepository = transferLimitRepository;
    }

    @Override
    public ApiResponse<TransferLimit> create(String code, String name, BigDecimal dailyMax) {
        if (this.transferLimitRepository.existsByCode(code)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Code '" + code + "' is already in use");
        }
        TransferLimit transferLimit = new TransferLimit();
        transferLimit.setCode(code);
        transferLimit.setName(name);
        transferLimit.setDailyMax(dailyMax);
        return ApiResponse.ok(this.transferLimitRepository.save(transferLimit));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransferLimit> get(UUID id) {
        return this.transferLimitRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TransferLimit> page(TransferLimitPageQuery query) {
        return this.transferLimitRepository.findPage(query);
    }

    @Override
    public ApiResponse<TransferLimit> update(UUID id, String code, String name, BigDecimal dailyMax, TransferLimitStatus status) {
        Optional<TransferLimit> found = this.transferLimitRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No transfer limit exists with id " + id);
        }
        TransferLimit transferLimit = found.get();
        if (!transferLimit.getCode().equals(code) && this.transferLimitRepository.existsByCode(code)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Code '" + code + "' is already in use");
        }
        transferLimit.setCode(code);
        transferLimit.setName(name);
        transferLimit.setDailyMax(dailyMax);
        if (status != null) {
            transferLimit.setStatus(status);
        }
        return ApiResponse.ok(this.transferLimitRepository.save(transferLimit));
    }

    @Override
    public ApiResponse<TransferLimit> delete(UUID id) {
        Optional<TransferLimit> found = this.transferLimitRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No transfer limit exists with id " + id);
        }
        TransferLimit transferLimit = found.get();
        this.transferLimitRepository.delete(transferLimit);
        return ApiResponse.ok(transferLimit);
    }

}
