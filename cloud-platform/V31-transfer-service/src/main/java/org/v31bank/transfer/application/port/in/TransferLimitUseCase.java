package org.v31bank.transfer.application.port.in;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.v31bank.transfer.application.dto.TransferLimitPageQuery;
import org.v31bank.transfer.domain.constant.TransferLimitStatus;
import org.v31bank.transfer.domain.model.TransferLimit;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Use cases for managing transfer limits.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface TransferLimitUseCase {

    /**
     * Add a transfer limit, provided its code is not already taken.
     * @param code the code, unique
     * @param name the display name
     * @param dailyMax the dailymax
     * @return the outcome of the command
     */
    ApiResponse<TransferLimit> create(String code, String name, BigDecimal dailyMax);

    Optional<TransferLimit> get(UUID id);

    /**
     * Find a page of transfer limits matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    PageResult<TransferLimit> page(TransferLimitPageQuery query);

    /**
     * Update the transfer limit with the given identifier.
     * @param id the record to update
     * @param code the code, which must not belong to another record
     * @param name the display name
     * @param dailyMax the dailymax
     * @param status the new status, or {@code null} to leave it unchanged
     * @return the outcome of the command
     */
    ApiResponse<TransferLimit> update(UUID id, String code, String name, BigDecimal dailyMax, TransferLimitStatus status);

    /**
     * Delete the transfer limit with the given identifier.
     * @param id the record to delete
     * @return the outcome of the command
     */
    ApiResponse<TransferLimit> delete(UUID id);

}
