package org.v31bank.ledger.presentation.dto;

import org.v31bank.ledger.domain.constant.LedgerAccountType;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;

/**
 * Request body for creating or updating a ledger account.
 *
 * @param code the code, unique
 * @param name the display name
 * @param type the type
 * @param status the status; ignored on create, where a record always starts
 * {@code ACTIVE}, and left unchanged on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record LedgerAccountRequest(String code, String name, LedgerAccountType type, LedgerAccountStatus status) {

}
