package org.v31bank.jooq.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordContext;
import org.jooq.RecordListener;

import org.v31bank.core.util.Uuids;

/**
 * Fills in the identifier and the audit columns as records are written, so that
 * no caller has to remember to, and none can quietly decline.
 * <p>
 * This is jOOQ's counterpart to Spring Data JPA's {@code AuditingEntityListener}.
 * It is not the same mechanism: JPA works from a mapped superclass and knows
 * every entity carries the columns, whereas a jOOQ record is generated from the
 * table it belongs to and may carry none of them. Each column is therefore
 * stamped only where the record declares it, which is what lets a schema adopt
 * auditing one table at a time.
 * <p>
 * It only sees writes made through jOOQ's record API — {@code store}, {@code
 * insert}, {@code update}, {@code merge}. A query written as
 * {@code dsl.insertInto(...)} bypasses it, because there is no record for it to
 * act on. Where the audit trail has to hold, write through records.
 * <p>
 * Timestamps come from a {@link Clock} fixed to UTC rather than from the host's
 * zone, so that rows written by nodes in different regions order against each
 * other.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class AuditRecordListener implements RecordListener {

    private final AuditorSupplier auditorSupplier;

    private final Clock clock;

    private final boolean assignIdentifiers;

    /**
     * Create a listener.
     * @param auditorSupplier answers who is acting
     * @param clock the source of the timestamps, expected to be UTC
     * @param assignIdentifiers whether to give a record without one a UUIDv7
     * primary key
     */
    public AuditRecordListener(AuditorSupplier auditorSupplier, Clock clock, boolean assignIdentifiers) {
        this.auditorSupplier = auditorSupplier;
        this.clock = clock;
        this.assignIdentifiers = assignIdentifiers;
    }

    @Override
    public void insertStart(RecordContext ctx) {
        stampCreation(ctx.record());
    }

    @Override
    public void updateStart(RecordContext ctx) {
        stampModification(ctx.record());
    }

    /**
     * Stamp an upsert, which may land as either an insert or an update.
     * <p>
     * The creation columns are filled only where the record left them empty, so a
     * merge that turns out to update an existing row does not rewrite when it was
     * created — while a merge that inserts still records it.
     */
    @Override
    public void mergeStart(RecordContext ctx) {
        Record record = ctx.record();
        stampCreationIfAbsent(record);
        stampModification(record);
    }

    /**
     * Record who created the row and when, and give it an identifier if it has
     * none.
     * <p>
     * The identifier is left alone when the caller already chose one: a transfer
     * whose identifier was issued upstream, so that a retry writes the same row
     * rather than a second one, must keep it.
     * @param record the record about to be inserted
     */
    void stampCreation(Record record) {
        Instant now = Instant.now(this.clock);
        String auditor = currentAuditor();
        if (this.assignIdentifiers) {
            stampIfAbsent(record, AuditColumns.ID, UUID.class, Uuids.timeOrdered());
        }
        stamp(record, AuditColumns.CREATED_BY, String.class, auditor);
        stamp(record, AuditColumns.CREATED_DATE, Instant.class, now);
        stamp(record, AuditColumns.LAST_MODIFIED_BY, String.class, auditor);
        stamp(record, AuditColumns.LAST_MODIFIED_DATE, Instant.class, now);
    }

    /**
     * Record who last changed the row and when, leaving the creation columns
     * untouched — they describe an event that has already happened.
     * @param record the record about to be updated
     */
    void stampModification(Record record) {
        stamp(record, AuditColumns.LAST_MODIFIED_BY, String.class, currentAuditor());
        stamp(record, AuditColumns.LAST_MODIFIED_DATE, Instant.class, Instant.now(this.clock));
    }

    private void stampCreationIfAbsent(Record record) {
        if (this.assignIdentifiers) {
            stampIfAbsent(record, AuditColumns.ID, UUID.class, Uuids.timeOrdered());
        }
        stampIfAbsent(record, AuditColumns.CREATED_BY, String.class, currentAuditor());
        stampIfAbsent(record, AuditColumns.CREATED_DATE, Instant.class, Instant.now(this.clock));
    }

    private String currentAuditor() {
        return this.auditorSupplier.currentAuditor().orElse(null);
    }

    /**
     * Set a column, where the record has it and there is something to record.
     * <p>
     * A missing value leaves the column as it is rather than writing null over
     * it: an unidentified caller is a reason not to claim who acted, not a reason
     * to erase who did.
     * @param record the record being written
     * @param column the column to set
     * @param type the column's type
     * @param value the value to record, ignored when {@code null}
     * @param <T> the column's type
     */
    private static <T> void stamp(Record record, String column, Class<T> type, T value) {
        if (value == null) {
            return;
        }
        Field<T> field = record.field(column, type);
        if (field != null) {
            record.set(field, value);
        }
    }

    /**
     * Set a column only where the record left it empty.
     * @param record the record being written
     * @param column the column to set
     * @param type the column's type
     * @param value the value to record, ignored when {@code null}
     * @param <T> the column's type
     */
    private static <T> void stampIfAbsent(Record record, String column, Class<T> type, T value) {
        if (value == null) {
            return;
        }
        Field<T> field = record.field(column, type);
        if (field != null && record.get(field) == null) {
            record.set(field, value);
        }
    }

}
