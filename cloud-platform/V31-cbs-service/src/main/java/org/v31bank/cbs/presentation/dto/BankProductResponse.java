package org.v31bank.cbs.presentation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;
import org.v31bank.cbs.domain.model.BankProduct;

/**
 * API representation of a bank product.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record BankProductResponse(UUID id, String code, String name, BankProductCategory category,
        BankProductStatus status, BigDecimal interestRate, Instant createdDate, Instant lastModifiedDate) {

    public static BankProductResponse from(BankProduct product) {
        return new BankProductResponse(product.getId(), product.getCode(), product.getName(), product.getCategory(),
                product.getStatus(), product.getInterestRate(), product.getCreatedDate(),
                product.getLastModifiedDate());
    }

}
