package org.v31bank.customer.presentation.dto;

import org.v31bank.customer.domain.constant.CustomerStatus;

/**
 * Request body for creating or updating a customer.
 *
 * @param email the customer email
 * @param fullName the customer full name
 * @param status the customer status; ignored on create
 * @author Xander Wang
 * @since 0.2.0
 */
public record CustomerRequest(String email, String fullName, CustomerStatus status) {

}
