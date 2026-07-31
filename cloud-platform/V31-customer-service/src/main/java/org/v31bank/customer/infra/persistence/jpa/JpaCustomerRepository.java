package org.v31bank.customer.infra.persistence.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.v31bank.customer.domain.model.Customer;

/**
 * Spring Data JPA repository for {@link Customer}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface JpaCustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

}
