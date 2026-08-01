package org.v31bank.compliance.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.compliance.application.dto.ComplianceCasePageQuery;
import org.v31bank.compliance.domain.model.ComplianceCase;
import org.v31bank.core.response.PageResponse;

/**
 * Output port for {@link ComplianceCase} persistence, implemented by the
 * infrastructure layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface ComplianceCasePort {

    /**
     * Insert a case that has no identifier, or update the one it names.
     * @param complianceCase the case to write
     * @return the case as it now stands, carrying the identifier and audit fields
     * filled in while writing it
     */
    ComplianceCase save(ComplianceCase complianceCase);

    Optional<ComplianceCase> findById(UUID id);

    /**
     * Find a page of cases matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching cases
     */
    PageResponse<ComplianceCase> findPage(ComplianceCasePageQuery query);

    /**
     * Whether any case already uses the given number.
     * @param caseNumber the number to check
     * @return {@code true} if the number is taken
     */
    boolean existsByCaseNumber(String caseNumber);

    void deleteById(UUID id);

}
