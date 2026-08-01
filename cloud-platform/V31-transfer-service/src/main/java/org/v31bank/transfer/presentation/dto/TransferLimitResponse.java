package org.v31bank.transfer.presentation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.v31bank.transfer.domain.constant.TransferLimitStatus;
import org.v31bank.transfer.domain.model.TransferLimit;

/**
 * API representation of a transfer limit.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record TransferLimitResponse(UUID id, String code, String name, BigDecimal dailyMax, TransferLimitStatus status, Instant createdDate,
        Instant lastModifiedDate) {

    public static TransferLimitResponse from(TransferLimit transferLimit) {
        return new TransferLimitResponse(transferLimit.getId(), transferLimit.getCode(), transferLimit.getName(), transferLimit.getDailyMax(), transferLimit.getStatus(),
                transferLimit.getCreatedDate(), transferLimit.getLastModifiedDate());
    }

}
