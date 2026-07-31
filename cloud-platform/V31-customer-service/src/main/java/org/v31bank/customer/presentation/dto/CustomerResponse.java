package org.v31bank.customer.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.customer.domain.constant.CustomerStatus;
import org.v31bank.customer.domain.model.Customer;

/**
 * API representation of a customer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record CustomerResponse(UUID id, String email, String fullName, CustomerStatus status,
        Instant createdDate, Instant lastModifiedDate) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getEmail(), customer.getFullName(),
                customer.getStatus(), customer.getCreatedDate(), customer.getLastModifiedDate());
    }

}
