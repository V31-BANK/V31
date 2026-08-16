/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.v31bank.data.jpa.autoconfigure;

import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.v31bank.data.jpa.audit.FixedAuditorAware;
import org.v31bank.data.jpa.domain.BaseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link V31DataJpaAutoConfiguration}.
 * <p>
 * Run against a real persistence unit rather than an empty context: auditing is only
 * meaningfully configured if a saved row comes back stamped, and
 * {@code @EnableJpaAuditing} cannot start without a metamodel to work against.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class V31DataJpaAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
				V31DataJpaAutoConfiguration.class))
		.withUserConfiguration(PersistenceConfiguration.class)
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:v31-audit;DB_CLOSE_DELAY=-1",
				"spring.jpa.hibernate.ddl-auto=create-drop");

	@Test
	void registersAnAuditorSoAuditColumnsAreNeverLeftUnset() {
		this.runner.run((context) -> {
			assertThat(context).hasSingleBean(AuditorAware.class);
			assertThat(auditor(context.getBean(AuditorAware.class))).contains("system");
		});
	}

	/**
	 * The point of the module: a row saved through an ordinary repository carries who
	 * wrote it and when, without the service having to remember to set either.
	 */
	@Test
	void stampsARowWithoutTheApplicationSettingAnything() {
		this.runner.run((context) -> {
			AuditedRecord saved = save(context.getBean(PlatformTransactionManager.class),
					context.getBean(EntityManagerFactory.class));
			assertThat(saved.getCreatedBy()).isEqualTo("system");
			assertThat(saved.getLastModifiedBy()).isEqualTo("system");
			assertThat(saved.getCreatedDate()).isNotNull();
			assertThat(saved.getLastModifiedDate()).isNotNull();
			assertThat(saved.getId()).isNotNull();
		});
	}

	@Test
	void appliesTheConfiguredAuditor() {
		this.runner.withPropertyValues("v31.data.jpa.auditing.default-auditor=batch")
			.run((context) -> assertThat(auditor(context.getBean(AuditorAware.class))).contains("batch"));
	}

	@Test
	void addsNothingWhenAuditingIsTurnedOff() {
		this.runner.withPropertyValues("v31.data.jpa.auditing.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(AuditorAware.class));
	}

	/**
	 * The fixed auditor stands in for a real one. A service that knows who is calling has
	 * to be able to say so without turning the rest of the module off.
	 */
	@Test
	void backsOffFromAnApplicationSuppliedAuditor() {
		this.runner.withUserConfiguration(CustomAuditorConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(AuditorAware.class);
			assertThat(auditor(context.getBean(AuditorAware.class))).contains("ada");
		});
	}

	@Test
	void bindsItsProperties() {
		this.runner.run((context) -> {
			V31DataJpaProperties properties = context.getBean(V31DataJpaProperties.class);
			assertThat(properties.getAuditing().isEnabled()).isTrue();
			assertThat(properties.getAuditing().getDefaultAuditor()).isEqualTo("system");
		});
	}

	private static AuditedRecord save(PlatformTransactionManager transactionManager,
			EntityManagerFactory entityManagerFactory) {
		EntityManager entityManager = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
		return new TransactionTemplate(transactionManager).execute((status) -> {
			AuditedRecord record = new AuditedRecord();
			entityManager.persist(record);
			entityManager.flush();
			return record;
		});
	}

	@SuppressWarnings("unchecked")
	private static Optional<String> auditor(AuditorAware<?> auditorAware) {
		return ((AuditorAware<String>) auditorAware).getCurrentAuditor();
	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = V31DataJpaAutoConfigurationTests.class)
	static class PersistenceConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAuditorConfiguration {

		@Bean
		AuditorAware<String> auditorAware() {
			return new FixedAuditorAware("ada");
		}

	}

	/**
	 * Stands in for any entity in the platform, all of which extend {@link BaseEntity}.
	 */
	@Entity
	@Table(name = "audited_record")
	static class AuditedRecord extends BaseEntity {

	}

}
