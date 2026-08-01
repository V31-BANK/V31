package org.v31bank.jooq.autoconfigure;

import java.util.Arrays;

import org.jooq.RecordListener;
import org.jooq.RecordListenerProvider;
import org.jooq.impl.DefaultConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.v31bank.jooq.audit.AuditRecordListener;
import org.v31bank.jooq.audit.AuditorSupplier;
import org.v31bank.jooq.audit.FixedAuditorSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link V31JooqAutoConfiguration}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class V31JooqAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(V31JooqAutoConfiguration.class));

    @Test
    void registersTheListenerAndAFallbackAuditor() {
        this.runner.run((context) -> {
            assertThat(context).hasSingleBean(AuditRecordListener.class);
            assertThat(context).hasSingleBean(AuditorSupplier.class);
            assertThat(context.getBean(AuditorSupplier.class).currentAuditor()).contains("system");
        });
    }

    @Test
    void recordsTheConfiguredDefaultAuditor() {
        this.runner.withPropertyValues("v31.jooq.auditing.default-auditor=migration").run((context) -> {
            assertThat(context.getBean(AuditorSupplier.class).currentAuditor()).contains("migration");
        });
    }

    @Test
    void backsOffFromAnApplicationSuppliedAuditor() {
        this.runner.withUserConfiguration(CustomAuditorConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(AuditorSupplier.class);
            assertThat(context.getBean(AuditorSupplier.class).currentAuditor()).contains("xander");
        });
    }

    @Test
    void addsNothingWhenAuditingIsTurnedOff() {
        this.runner.withPropertyValues("v31.jooq.auditing.enabled=false").run((context) -> {
            assertThat(context).doesNotHaveBean(AuditRecordListener.class);
            assertThat(context).doesNotHaveBean(DefaultConfigurationCustomizer.class);
        });
    }

    @Test
    void attachesTheListenerToTheJooqConfiguration() {
        this.runner.run((context) -> {
            DefaultConfiguration configuration = new DefaultConfiguration();
            context.getBean(DefaultConfigurationCustomizer.class).customize(configuration);
            assertThat(providedListeners(configuration)).containsExactly(context.getBean(AuditRecordListener.class));
        });
    }

    @Test
    void keepsListenersRegisteredByTheApplication() {
        RecordListener existing = RecordListener.onInsertStart((ctx) -> {
        });
        this.runner.run((context) -> {
            DefaultConfiguration configuration = new DefaultConfiguration();
            configuration.set(existing);
            context.getBean(DefaultConfigurationCustomizer.class).customize(configuration);
            assertThat(providedListeners(configuration)).containsExactly(existing,
                    context.getBean(AuditRecordListener.class));
        });
    }

    private static Object[] providedListeners(DefaultConfiguration configuration) {
        return Arrays.stream(configuration.recordListenerProviders())
            .map(RecordListenerProvider::provide)
            .toArray();
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAuditorConfiguration {

        @Bean
        AuditorSupplier auditorSupplier() {
            return new FixedAuditorSupplier("xander");
        }

    }

}
