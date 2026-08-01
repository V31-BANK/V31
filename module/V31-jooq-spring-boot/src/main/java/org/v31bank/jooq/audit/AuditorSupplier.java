package org.v31bank.jooq.audit;

import java.util.Optional;

/**
 * Answers who is acting, so that {@link AuditRecordListener} can record it
 * against every row a request writes.
 * <p>
 * This is jOOQ's counterpart to Spring Data's {@code AuditorAware}, declared here
 * rather than reused so that a service using jOOQ alone does not have to bring in
 * Spring Data to say who its user is. An application backed by both implements
 * the two against the same source.
 * <p>
 * The auditor is read once per statement, on the thread running it. An
 * implementation reading from a security context or a request scope has to
 * account for work handed to another thread, where that context is not
 * automatically present.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@FunctionalInterface
public interface AuditorSupplier {

    /**
     * Return who is currently acting.
     * @return the auditor, or empty when nobody is identified — a scheduled job,
     * a migration, a request that has not been authenticated. The audit columns
     * are then left as they are rather than being filled with a placeholder.
     */
    Optional<String> currentAuditor();

}
