package org.v31bank.transfer.presentation.dto;

import java.math.BigDecimal;
import org.v31bank.transfer.domain.constant.TransferLimitStatus;

/**
 * Request body for creating or updating a transfer limit.
 *
 * @param code the code, unique
 * @param name the display name
 * @param dailyMax the dailymax
 * @param status the status; ignored on create, where a record always starts
 * {@code ACTIVE}, and left unchanged on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record TransferLimitRequest(String code, String name, BigDecimal dailyMax, TransferLimitStatus status) {

}
