package org.v31bank.notification.presentation.dto;

/**
 * Request body for creating or updating a ledger account through this service.
 *
 * @param code the code the account is known by, unique in the ledger
 * @param name the display name
 * @param type which side of the balance sheet it belongs to, for example
 * {@code ASSET}
 * @param status the status; ignored on create, and left unchanged on update when
 * absent
 * @author Xander Wang
 * @since 0.2.0
 */
public record LedgerAccountRequest(String code, String name, String type, String status) {

}
