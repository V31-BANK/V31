package org.v31bank.risk.infra.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import org.v31bank.risk.application.dto.RiskRulePageQuery;
import org.v31bank.risk.application.port.out.RiskRulePort;
import org.v31bank.risk.domain.model.RiskRule;
import org.v31bank.risk.infra.persistence.jpa.JpaRiskRuleRepository;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * {@link RiskRulePort} adapter backed by Spring Data JPA.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Repository
public class RiskRulePersistenceAdapter implements RiskRulePort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdDate");

    private final JpaRiskRuleRepository jpaRepository;

    public RiskRulePersistenceAdapter(JpaRiskRuleRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RiskRule save(RiskRule riskRule) {
        return this.jpaRepository.save(riskRule);
    }

    @Override
    public Optional<RiskRule> findById(UUID id) {
        return this.jpaRepository.findById(id);
    }

    @Override
    public PageResult<RiskRule> findPage(RiskRulePageQuery query) {
        Specification<RiskRule> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getCode())) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + query.getCode().toLowerCase() + "%"));
            }
            if (query.getSeverity() != null) {
                predicates.add(cb.equal(root.get("severity"), query.getSeverity()));
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
    public void delete(RiskRule riskRule) {
        this.jpaRepository.delete(riskRule);
    }

}
