package org.v31bank.jooq.autoconfigure;

import java.time.Clock;

import org.jooq.DSLContext;
import org.jooq.RecordListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;

import org.v31bank.jooq.audit.AuditRecordListener;
import org.v31bank.jooq.audit.AuditorSupplier;
import org.v31bank.jooq.audit.FixedAuditorSupplier;

/**
 * {@link AutoConfiguration Auto-configuration} for V31 jOOQ support: registers a
 * {@link RecordListener} that fills in the identifier and the audit columns as
 * records are written, along with a fallback {@link AuditorSupplier} that says
 * who is acting.
 * <p>
 * The listener is attached through a {@link DefaultConfigurationCustomizer},
 * which is the seam Spring Boot leaves for adding to the jOOQ configuration it
 * builds. It is appended rather than set, so a listener registered by the
 * application or by another library survives.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@AutoConfiguration(before = JooqAutoConfiguration.class)
@ConditionalOnClass({ DSLContext.class, DefaultConfigurationCustomizer.class })
@ConditionalOnBooleanProperty(name = "v31.jooq.auditing.enabled", matchIfMissing = true)
@EnableConfigurationProperties(V31JooqProperties.class)
public class V31JooqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditorSupplier auditorSupplier(V31JooqProperties properties) {
        return new FixedAuditorSupplier(properties.getAuditing().getDefaultAuditor());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditRecordListener auditRecordListener(AuditorSupplier auditorSupplier, V31JooqProperties properties) {
        return new AuditRecordListener(auditorSupplier, Clock.systemUTC(),
                properties.getIdentifiers().isEnabled());
    }

    @Bean
    public DefaultConfigurationCustomizer v31JooqAuditConfigurationCustomizer(AuditRecordListener listener) {
        return (configuration) -> configuration.setAppending(listener);
    }

}
