package org.v31bank.customer.infra.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import org.v31bank.customer.application.dto.CustomerCategoryPageQuery;
import org.v31bank.customer.application.port.out.CustomerCategoryPort;
import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;
import org.v31bank.customer.infra.persistence.jpa.JpaCustomerCategoryRepository;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * {@link CustomerCategoryPort} adapter backed by Spring Data JPA.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Repository
public class CustomerCategoryPersistenceAdapter implements CustomerCategoryPort {

    /**
     * Sibling ordering. PostgreSQL sorts nulls last on an ascending column, which
     * matches how unpositioned nodes are ordered once assembled into a tree.
     */
    private static final Sort SIBLING_ORDER = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("code"));

    private final JpaCustomerCategoryRepository jpaRepository;

    public CustomerCategoryPersistenceAdapter(JpaCustomerCategoryRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CustomerCategory save(CustomerCategory category) {
        return this.jpaRepository.save(category);
    }

    @Override
    public Optional<CustomerCategory> findById(UUID id) {
        return this.jpaRepository.findById(id);
    }

    @Override
    public List<CustomerCategory> findAll(CustomerCategoryStatus status) {
        return (status != null) ? this.jpaRepository.findAllByStatus(status, SIBLING_ORDER)
                : this.jpaRepository.findAll(SIBLING_ORDER);
    }

    @Override
    public PageResult<CustomerCategory> findPage(CustomerCategoryPageQuery query) {
        Specification<CustomerCategory> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getCode())) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + query.getCode().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(query.getName())) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + query.getName().toLowerCase() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.isRootOnly()) {
                predicates.add(cb.isNull(root.get("parentId")));
            }
            else if (query.getParentId() != null) {
                predicates.add(cb.equal(root.get("parentId"), query.getParentId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResult.of(this.jpaRepository.findAll(spec, query.toPageable(SIBLING_ORDER)));
    }

    @Override
    public boolean existsByParentId(UUID parentId) {
        return this.jpaRepository.existsByParentId(parentId);
    }

    @Override
    public boolean existsByCode(String code) {
        return this.jpaRepository.existsByCode(code);
    }

    @Override
    public void delete(CustomerCategory category) {
        this.jpaRepository.delete(category);
    }

}
