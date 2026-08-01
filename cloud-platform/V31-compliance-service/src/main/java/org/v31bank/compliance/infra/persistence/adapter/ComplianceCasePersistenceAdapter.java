package org.v31bank.compliance.infra.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectLimitStep;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import org.v31bank.compliance.application.dto.ComplianceCasePageQuery;
import org.v31bank.compliance.application.port.out.ComplianceCasePort;
import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;
import org.v31bank.compliance.domain.model.ComplianceCase;
import org.v31bank.compliance.infra.persistence.jooq.ComplianceCaseRecord;
import org.v31bank.compliance.infra.persistence.jooq.ComplianceCaseTable;
import org.v31bank.core.response.PageResponse;
import org.v31bank.jooq.util.JooqPages;

/**
 * {@link ComplianceCasePort} adapter backed by jOOQ.
 * <p>
 * Writes go through the record API — {@code insert()} and {@code update()} on an
 * updatable record — rather than through {@code dsl.insertInto(...)}, because
 * only the record API passes through the audit listener the jOOQ starter
 * registers. A statement written the other way would save the row and leave no
 * trace of who wrote it.
 * <p>
 * This is also where the domain model and the generated-style record meet.
 * Neither knows about the other: the domain has no jOOQ types on it, and the
 * record has no idea what a {@link ComplianceCaseStatus} is — it holds the text
 * the column stores.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Repository
public class ComplianceCasePersistenceAdapter implements ComplianceCasePort {

    private static final ComplianceCaseTable CASE = ComplianceCaseTable.COMPLIANCE_CASE;

    private final DSLContext dsl;

    public ComplianceCasePersistenceAdapter(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public ComplianceCase save(ComplianceCase complianceCase) {
        return (complianceCase.getId() != null) ? update(complianceCase) : insert(complianceCase);
    }

    @Override
    public Optional<ComplianceCase> findById(UUID id) {
        return Optional.ofNullable(this.dsl.fetchOne(CASE, CASE.ID.eq(id))).map(ComplianceCasePersistenceAdapter::toDomain);
    }

    @Override
    public PageResponse<ComplianceCase> findPage(ComplianceCasePageQuery query) {
        SelectLimitStep<ComplianceCaseRecord> select = this.dsl.selectFrom(CASE)
            .where(conditions(query))
            .orderBy(CASE.CREATED_DATE.desc(), CASE.ID.desc());
        return JooqPages.fetch(this.dsl, select, query.getPageNumber(), query.getPageSize())
            .map(ComplianceCasePersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByCaseNumber(String caseNumber) {
        return this.dsl.fetchExists(CASE, CASE.CASE_NUMBER.eq(caseNumber));
    }

    @Override
    public void deleteById(UUID id) {
        this.dsl.deleteFrom(CASE).where(CASE.ID.eq(id)).execute();
    }

    private ComplianceCase insert(ComplianceCase complianceCase) {
        ComplianceCaseRecord record = this.dsl.newRecord(CASE);
        apply(record, complianceCase);
        record.insert();
        return toDomain(record);
    }

    /**
     * Update the named row.
     * <p>
     * The record is built rather than fetched, and its key is marked unchanged so
     * that it identifies the row in the {@code where} clause instead of being
     * written into the {@code set} clause.
     * @param complianceCase the case to write, carrying the identifier of the row
     * @return the case as it now stands
     */
    private ComplianceCase update(ComplianceCase complianceCase) {
        ComplianceCaseRecord record = this.dsl.newRecord(CASE);
        record.set(CASE.ID, complianceCase.getId());
        record.touched(CASE.ID, false);
        apply(record, complianceCase);
        record.update();
        return toDomain(record);
    }

    /**
     * Filters carried by the query, each applied only when it was supplied.
     * <p>
     * The case number is matched with {@code containsIgnoreCase} rather than a
     * hand-built {@code like}, so that a {@code %} typed into the search box is
     * matched literally instead of turning into a wildcard that scans the table.
     * @param query the query to read the filters from
     * @return the conditions to apply, empty when nothing was filtered on
     */
    private static List<Condition> conditions(ComplianceCasePageQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (StringUtils.hasText(query.getCaseNumber())) {
            conditions.add(CASE.CASE_NUMBER.containsIgnoreCase(query.getCaseNumber()));
        }
        if (query.getCustomerId() != null) {
            conditions.add(CASE.CUSTOMER_ID.eq(query.getCustomerId()));
        }
        if (query.getType() != null) {
            conditions.add(CASE.TYPE.eq(query.getType().name()));
        }
        if (query.getStatus() != null) {
            conditions.add(CASE.STATUS.eq(query.getStatus().name()));
        }
        return conditions;
    }

    private static void apply(ComplianceCaseRecord record, ComplianceCase complianceCase) {
        record.set(CASE.CASE_NUMBER, complianceCase.getCaseNumber());
        record.set(CASE.CUSTOMER_ID, complianceCase.getCustomerId());
        record.set(CASE.TYPE, complianceCase.getType().name());
        record.set(CASE.STATUS, complianceCase.getStatus().name());
        record.set(CASE.SUMMARY, complianceCase.getSummary());
    }

    private static ComplianceCase toDomain(ComplianceCaseRecord record) {
        ComplianceCase complianceCase = new ComplianceCase();
        complianceCase.setId(record.get(CASE.ID));
        complianceCase.setCaseNumber(record.get(CASE.CASE_NUMBER));
        complianceCase.setCustomerId(record.get(CASE.CUSTOMER_ID));
        complianceCase.setType(ComplianceCaseType.valueOf(record.get(CASE.TYPE)));
        complianceCase.setStatus(ComplianceCaseStatus.valueOf(record.get(CASE.STATUS)));
        complianceCase.setSummary(record.get(CASE.SUMMARY));
        complianceCase.setCreatedBy(record.get(CASE.CREATED_BY));
        complianceCase.setCreatedDate(record.get(CASE.CREATED_DATE));
        complianceCase.setLastModifiedBy(record.get(CASE.LAST_MODIFIED_BY));
        complianceCase.setLastModifiedDate(record.get(CASE.LAST_MODIFIED_DATE));
        return complianceCase;
    }

}
