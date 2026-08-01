package org.v31bank.transfer.infra.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import org.v31bank.transfer.application.dto.TransferLimitPageQuery;
import org.v31bank.transfer.application.port.out.TransferLimitPort;
import org.v31bank.transfer.domain.model.TransferLimit;
import org.v31bank.transfer.infra.persistence.jpa.JpaTransferLimitRepository;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * {@link TransferLimitPort} adapter backed by Spring Data JPA.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Repository
public class TransferLimitPersistenceAdapter implements TransferLimitPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdDate");

    private final JpaTransferLimitRepository jpaRepository;

    public TransferLimitPersistenceAdapter(JpaTransferLimitRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TransferLimit save(TransferLimit transferLimit) {
        return this.jpaRepository.save(transferLimit);
    }

    @Override
    public Optional<TransferLimit> findById(UUID id) {
        return this.jpaRepository.findById(id);
    }

    @Override
    public PageResult<TransferLimit> findPage(TransferLimitPageQuery query) {
        Specification<TransferLimit> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getCode())) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + query.getCode().toLowerCase() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResult.of(this.jpaRepository.findAll(spec, query.toPageable(NEWEST_FIRST)));
    }

    @Override
    public boolean existsByCode(String code) {
        return this.jpaRepository.existsByCode(code);
    }

    @Override
    public void delete(TransferLimit transferLimit) {
        this.jpaRepository.delete(transferLimit);
    }

}
