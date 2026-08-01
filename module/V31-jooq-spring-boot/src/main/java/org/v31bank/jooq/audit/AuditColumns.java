package org.v31bank.jooq.audit;

/**
 * Names of the columns {@link AuditRecordListener} maintains.
 * <p>
 * jOOQ works from generated classes rather than a mapped superclass, so there is
 * no {@code BaseEntity} to inherit these from and nothing forces a table to carry
 * them. The listener therefore stamps whichever of them a record happens to
 * declare, and skips the rest.
 * <p>
 * The names match the columns {@code org.v31bank.data.jpa.domain.BaseEntity}
 * maps, so a table can be reached through either mapper and read the same way.
 * Changing one of these means changing every table already carrying it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class AuditColumns {

    /**
     * Primary key, a time-ordered UUIDv7.
     */
    public static final String ID = "id";

    /**
     * Who inserted the row.
     */
    public static final String CREATED_BY = "created_by";

    /**
     * When the row was inserted, as an instant.
     */
    public static final String CREATED_DATE = "created_date";

    /**
     * Who last changed the row.
     */
    public static final String LAST_MODIFIED_BY = "last_modified_by";

    /**
     * When the row was last changed, as an instant.
     */
    public static final String LAST_MODIFIED_DATE = "last_modified_date";

    private AuditColumns() {
    }

}
