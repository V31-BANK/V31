package org.v31bank.customer.infra.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;

/**
 * Spring Data JPA repository for {@link CustomerCategory}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface JpaCustomerCategoryRepository
        extends JpaRepository<CustomerCategory, UUID>, JpaSpecificationExecutor<CustomerCategory> {

    List<CustomerCategory> findAllByStatus(CustomerCategoryStatus status, Sort sort);

    boolean existsByParentId(UUID parentId);

    boolean existsByCode(String code);

}
