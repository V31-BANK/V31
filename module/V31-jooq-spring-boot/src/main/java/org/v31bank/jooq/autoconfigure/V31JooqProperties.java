package org.v31bank.jooq.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for V31 jOOQ.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@ConfigurationProperties("v31.jooq")
public class V31JooqProperties {

    private final Auditing auditing = new Auditing();

    private final Identifiers identifiers = new Identifiers();

    public Auditing getAuditing() {
        return this.auditing;
    }

    public Identifiers getIdentifiers() {
        return this.identifiers;
    }

    /**
     * Auditing properties.
     */
    public static class Auditing {

        /**
         * Whether to record who wrote each row and when.
         */
        private boolean enabled = true;

        /**
         * Auditor recorded when no application-provided AuditorSupplier bean
         * exists.
         */
        private String defaultAuditor = "system";

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultAuditor() {
            return this.defaultAuditor;
        }

        public void setDefaultAuditor(String defaultAuditor) {
            this.defaultAuditor = defaultAuditor;
        }

    }

    /**
     * Primary key properties.
     */
    public static class Identifiers {

        /**
         * Whether to give a record without a primary key a time-ordered UUIDv7.
         * Disable where identifiers come from the database instead, such as from
         * a sequence or a column default.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
