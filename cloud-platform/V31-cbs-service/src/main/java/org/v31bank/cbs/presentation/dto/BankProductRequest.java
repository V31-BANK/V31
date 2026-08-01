package org.v31bank.cbs.presentation.dto;

import java.math.BigDecimal;

import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;

/**
 * Request body for adding or updating a bank product.
 *
 * @param code the code the product is known by, unique
 * @param name the display name
 * @param category what kind of account it opens
 * @param status the status; ignored on create, where a product always starts
 * {@code DRAFT}, and left unchanged on update when {@code null}
 * @param interestRate the annual rate as a fraction, so 2.5% is {@code 0.025}
 * @author Xander Wang
 * @since 0.2.0
 */
public record BankProductRequest(String code, String name, BankProductCategory category, BankProductStatus status,
        BigDecimal interestRate) {

}
