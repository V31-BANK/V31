package org.v31bank.risk.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.risk.application.dto.RiskRulePageQuery;
import org.v31bank.risk.domain.model.RiskRule;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Output port for {@link RiskRule} persistence, implemented by the infrastructure
 * layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface RiskRulePort {

    RiskRule save(RiskRule riskRule);

    Optional<RiskRule> findById(UUID id);

    /**
     * Find a page of records matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    PageResult<RiskRule> findPage(RiskRulePageQuery query);

    /**
     * Whether any record already uses the given code.
     * @param code the value to check
     * @return {@code true} if it is taken
     */
    boolean existsByCode(String code);

    void delete(RiskRule riskRule);

}
