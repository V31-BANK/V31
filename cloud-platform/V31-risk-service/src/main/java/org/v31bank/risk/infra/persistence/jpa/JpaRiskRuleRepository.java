package org.v31bank.risk.infra.persistence.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.v31bank.risk.domain.model.RiskRule;

/**
 * Spring Data JPA repository for {@link RiskRule}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface JpaRiskRuleRepository extends JpaRepository<RiskRule, UUID>, JpaSpecificationExecutor<RiskRule> {

    boolean existsByCode(String code);

}
