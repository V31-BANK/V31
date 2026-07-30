package org.v31bank.customer.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.customer.domain.constant.CustomerStatus;
import org.v31bank.customer.domain.model.Customer;
import org.v31bank.data.jpa.domain.PageQuery;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Output port for {@link Customer} persistence, implemented by the
 * infrastructure layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface CustomerPort {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    /**
     * Find a page of customers, optionally filtered.
     * @param email email fragment to match, or {@code null} for no filter
     * @param status status to match, or {@code null} for no filter
     * @param pageQuery the pagination request
     * @return the page of matching customers, newest first
     */
    PageResult<Customer> findPage(String email, CustomerStatus status, PageQuery pageQuery);

    void delete(Customer customer);

}
