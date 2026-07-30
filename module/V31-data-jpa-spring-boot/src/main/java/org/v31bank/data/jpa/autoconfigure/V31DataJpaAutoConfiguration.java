package org.v31bank.data.jpa.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.v31bank.data.jpa.audit.FixedAuditorAware;

/**
 * {@link AutoConfiguration Auto-configuration} for V31 Data JPA support: enables
 * JPA auditing and registers a fallback {@link AuditorAware} so audit fields on
 * {@link org.v31bank.data.jpa.domain.BaseEntity} are populated automatically.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@AutoConfiguration
@ConditionalOnClass(AuditingEntityListener.class)
@ConditionalOnBooleanProperty(name = "v31.data.jpa.auditing.enabled", matchIfMissing = true)
@EnableConfigurationProperties(V31DataJpaProperties.class)
@EnableJpaAuditing
public class V31DataJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<String> auditorAware(V31DataJpaProperties properties) {
        return new FixedAuditorAware(properties.getAuditing().getDefaultAuditor());
    }

}
