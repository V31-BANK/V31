package org.v31bank.compliance.infra.persistence.jooq;

import java.io.Serial;

import org.jooq.impl.UpdatableRecordImpl;

/**
 * A row of {@code compliance_case}.
 * <p>
 * Being an updatable record — rather than a plain one — is what makes
 * {@code insert()}, {@code update()} and {@code store()} available on it, and
 * those are the calls the audit listener registered by the jOOQ starter observes.
 * Writing the same row through {@code dsl.insertInto(...)} would work and would
 * not be audited.
 * <p>
 * It carries no typed accessors: the adapter reads and writes through the fields
 * on {@link ComplianceCaseTable}, which keeps this class to what jOOQ needs to
 * instantiate it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ComplianceCaseRecord extends UpdatableRecordImpl<ComplianceCaseRecord> {

    @Serial
    private static final long serialVersionUID = 1L;

    public ComplianceCaseRecord() {
        super(ComplianceCaseTable.COMPLIANCE_CASE);
    }

}
