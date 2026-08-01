package org.v31bank.jooq.audit;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import org.v31bank.core.util.Uuids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AuditRecordListener}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class AuditRecordListenerTests {

    private static final Instant NOW = Instant.parse("2026-08-01T09:00:00Z");

    private static final Field<UUID> ID = DSL.field(DSL.name(AuditColumns.ID), UUID.class);

    private static final Field<String> CREATED_BY = DSL.field(DSL.name(AuditColumns.CREATED_BY), String.class);

    private static final Field<Instant> CREATED_DATE = DSL.field(DSL.name(AuditColumns.CREATED_DATE), Instant.class);

    private static final Field<String> LAST_MODIFIED_BY = DSL.field(DSL.name(AuditColumns.LAST_MODIFIED_BY),
            String.class);

    private static final Field<Instant> LAST_MODIFIED_DATE = DSL.field(DSL.name(AuditColumns.LAST_MODIFIED_DATE),
            Instant.class);

    private static final Field<String> EMAIL = DSL.field(DSL.name("email"), String.class);

    private final DSLContext dsl = DSL.using(SQLDialect.POSTGRES);

    private final AuditRecordListener listener = listener(() -> Optional.of("xander"));

    @Test
    void stampsEveryAuditColumnOnCreation() {
        Record record = auditedRecord();
        this.listener.stampCreation(record);
        assertEquals("xander", record.get(CREATED_BY));
        assertEquals(NOW, record.get(CREATED_DATE));
        assertEquals("xander", record.get(LAST_MODIFIED_BY));
        assertEquals(NOW, record.get(LAST_MODIFIED_DATE));
    }

    @Test
    void issuesATimeOrderedIdentifierOnCreation() {
        Record record = auditedRecord();
        this.listener.stampCreation(record);
        UUID id = record.get(ID);
        assertNotNull(id);
        assertEquals(7, id.version());
        assertTrue(Uuids.timestampOf(id).isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void keepsAnIdentifierTheCallerAlreadyChose() {
        Record record = auditedRecord();
        UUID chosen = UUID.fromString("019fb995-685c-77eb-8f95-c62642e1c17e");
        record.set(ID, chosen);
        this.listener.stampCreation(record);
        assertEquals(chosen, record.get(ID));
    }

    @Test
    void leavesTheIdentifierAloneWhenTheDatabaseIssuesIt() {
        Record record = auditedRecord();
        listener(() -> Optional.of("xander"), false).stampCreation(record);
        assertNull(record.get(ID));
        assertEquals(NOW, record.get(CREATED_DATE));
    }

    @Test
    void touchesOnlyTheModificationColumnsOnUpdate() {
        Record record = auditedRecord();
        this.listener.stampModification(record);
        assertNull(record.get(CREATED_BY));
        assertNull(record.get(CREATED_DATE));
        assertEquals("xander", record.get(LAST_MODIFIED_BY));
        assertEquals(NOW, record.get(LAST_MODIFIED_DATE));
    }

    @Test
    void doesNotEraseWhoActedWhenNobodyIsIdentified() {
        Record record = auditedRecord();
        record.set(LAST_MODIFIED_BY, "xander");
        listener(Optional::empty).stampModification(record);
        assertEquals("xander", record.get(LAST_MODIFIED_BY));
        assertEquals(NOW, record.get(LAST_MODIFIED_DATE));
    }

    @Test
    void leavesATableWithoutAuditColumnsAlone() {
        Record record = this.dsl.newRecord(EMAIL);
        record.set(EMAIL, "xander.wang@v31bank.org");
        this.listener.stampCreation(record);
        this.listener.stampModification(record);
        assertEquals("xander.wang@v31bank.org", record.get(EMAIL));
        assertEquals(1, record.size());
    }

    @Test
    void stampsTheColumnsATableDoesCarry() {
        Record record = this.dsl.newRecord(EMAIL, LAST_MODIFIED_DATE);
        this.listener.stampModification(record);
        assertEquals(NOW, record.get(LAST_MODIFIED_DATE));
    }

    private Record auditedRecord() {
        return this.dsl.newRecord(ID, CREATED_BY, CREATED_DATE, LAST_MODIFIED_BY, LAST_MODIFIED_DATE, EMAIL);
    }

    private AuditRecordListener listener(AuditorSupplier auditorSupplier) {
        return listener(auditorSupplier, true);
    }

    private AuditRecordListener listener(AuditorSupplier auditorSupplier, boolean assignIdentifiers) {
        return new AuditRecordListener(auditorSupplier, Clock.fixed(NOW, ZoneOffset.UTC), assignIdentifiers);
    }

}
