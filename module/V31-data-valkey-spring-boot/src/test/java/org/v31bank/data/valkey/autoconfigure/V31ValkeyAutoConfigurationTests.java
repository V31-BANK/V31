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

package org.v31bank.data.valkey.autoconfigure;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import org.v31bank.data.valkey.cache.ValkeyCacheErrorHandler;
import org.v31bank.data.valkey.lock.ValkeyLock;
import org.v31bank.data.valkey.util.ValkeyKeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link V31ValkeyAutoConfiguration} and
 * {@link V31ValkeyCacheAutoConfiguration}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class V31ValkeyAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(V31ValkeyAutoConfiguration.class, V31ValkeyCacheAutoConfiguration.class))
		.withUserConfiguration(RedisConnectionConfiguration.class);

	@Test
	void registersTheTemplateKeyBuilderAndLock() {
		this.runner.run((context) -> {
			assertThat(context).hasBean("redisTemplate");
			assertThat(context).hasSingleBean(ValkeyKeys.class);
			assertThat(context).hasSingleBean(ValkeyLock.class);
			assertThat(context.getBean(ValkeyKeys.class).of("customer", "7")).isEqualTo("v31:customer:7");
		});
	}

	@Test
	void writesReadableKeysRatherThanSerialisedOnes() {
		this.runner.run((context) -> {
			RedisTemplate<?, ?> template = context.getBean("redisTemplate", RedisTemplate.class);
			assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
			assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
			assertThat(template.getValueSerializer()).isSameAs(context.getBean(RedisSerializer.class));
		});
	}

	@Test
	void takesTheNameFromSpringBootsJdkSerialisingTemplate() {
		this.runner.withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class)).run((context) -> {
			RedisTemplate<?, ?> template = context.getBean("redisTemplate", RedisTemplate.class);
			assertThat(template.getValueSerializer()).isNotInstanceOf(JdkSerializationRedisSerializer.class);
			assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
		});
	}

	@Test
	void appliesTheConfiguredKeyPrefix() {
		this.runner.withPropertyValues("v31.data.valkey.key-prefix=wallet")
			.run((context) -> assertThat(context.getBean(ValkeyKeys.class).of("balance")).isEqualTo("wallet:balance"));
	}

	@Test
	void readsBackAValueOfATrustedType() {
		this.runner.run((context) -> {
			RedisSerializer<Object> serializer = serializer(context.getBean(RedisSerializer.class));
			byte[] written = serializer.serialize(new Balance("BTC", "1.25"));
			assertThat(serializer.deserialize(written)).isEqualTo(new Balance("BTC", "1.25"));
		});
	}

	/**
	 * The platform stores instants and decimal amounts everywhere. A serializer that
	 * turns an {@code Instant} into a number of seconds, or a {@code BigDecimal} into a
	 * double, corrupts the value silently — which is the whole reason those types were
	 * chosen.
	 */
	@Test
	void readsBackInstantsAndAmountsUnchanged() {
		this.runner.run((context) -> {
			RedisSerializer<Object> serializer = serializer(context.getBean(RedisSerializer.class));
			Movement movement = new Movement(Instant.parse("2026-08-01T09:00:00.123456Z"),
					new BigDecimal("0.00000001"));
			Object read = serializer.deserialize(serializer.serialize(movement));
			assertThat(read).isEqualTo(movement);
			assertThat(((Movement) read).amount()).isEqualByComparingTo("0.00000001");
			assertThat(((Movement) read).at()).isEqualTo(Instant.parse("2026-08-01T09:00:00.123456Z"));
		});
	}

	@Test
	void registersAnErrorHandlerThatKeepsAnOutageOutOfTheCallPath() {
		this.runner.run((context) -> {
			assertThat(context).hasSingleBean(CachingConfigurer.class);
			CacheErrorHandler handler = context.getBean(CachingConfigurer.class).errorHandler();
			assertThat(handler).isInstanceOf(ValkeyCacheErrorHandler.class);
			assertThatNoException().isThrownBy(() -> handler.handleCacheGetError(new QueryTimeoutException("down"),
					new ConcurrentMapCache("customers"), "7"));
		});
	}

	@Test
	void refusesToReadBackAValueOfAnUntrustedType() {
		this.runner.run((context) -> {
			byte[] written = serializer(context.getBean(RedisSerializer.class)).serialize(new Balance("BTC", "1.25"));
			RedisSerializer<Object> restricted = new V31ValkeyAutoConfiguration()
				.valkeyValueSerializer(propertiesTrusting("org.v31bank.nothing.at.all"));
			assertThatExceptionOfType(SerializationException.class).isThrownBy(() -> restricted.deserialize(written));
		});
	}

	@Test
	void backsOffFromAnApplicationSuppliedTemplate() {
		this.runner.withUserConfiguration(CustomTemplateConfiguration.class)
			.run((context) -> assertThat(context.getBean("redisTemplate"))
				.isSameAs(context.getBean(CustomTemplateConfiguration.class)
					.redisTemplate(context.getBean(RedisConnectionFactory.class))));
	}

	@Test
	void givesEveryCacheAPrefixAndAnExpiry() {
		this.runner.run((context) -> {
			RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
			assertThat(configuration.getKeyPrefixFor("customers")).startsWith("v31:cache:");
			assertThat(configuration.getTtlFunction().getTimeToLive(Object.class, null))
				.isEqualTo(Duration.ofMinutes(10));
		});
	}

	@Test
	void appliesTheConfiguredDefaultExpiry() {
		this.runner.withPropertyValues("v31.data.valkey.cache.default-ttl=30s")
			.run((context) -> assertThat(
					context.getBean(RedisCacheConfiguration.class).getTtlFunction().getTimeToLive(Object.class, null))
				.isEqualTo(Duration.ofSeconds(30)));
	}

	@Test
	void cachesMissesUnlessToldNotTo() {
		this.runner
			.run((context) -> assertThat(context.getBean(RedisCacheConfiguration.class).getAllowCacheNullValues())
				.isTrue());
		this.runner.withPropertyValues("v31.data.valkey.cache.allow-null-values=false")
			.run((context) -> assertThat(context.getBean(RedisCacheConfiguration.class).getAllowCacheNullValues())
				.isFalse());
	}

	@Test
	void addsNoCachingWhenItIsTurnedOff() {
		this.runner.withPropertyValues("v31.data.valkey.cache.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(RedisCacheConfiguration.class));
	}

	@SuppressWarnings("unchecked")
	private static RedisSerializer<Object> serializer(RedisSerializer<?> serializer) {
		return (RedisSerializer<Object>) serializer;
	}

	private static V31ValkeyProperties propertiesTrusting(String... packages) {
		V31ValkeyProperties properties = new V31ValkeyProperties();
		properties.getSerialization().setTrustedPackages(List.of(packages));
		return properties;
	}

	/**
	 * A value of a type under {@code org.v31bank}, which the default trusted packages
	 * cover.
	 */
	public record Balance(String asset, String amount) {

	}

	/**
	 * Carries the two types the platform is most exposed to losing precision on.
	 */
	public record Movement(Instant at, BigDecimal amount) {

	}

	@Configuration(proxyBeanMethods = false)
	static class RedisConnectionConfiguration {

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
		}

		@Bean
		StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
			return new StringRedisTemplate(connectionFactory);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTemplateConfiguration {

		private final RedisTemplate<String, Object> template = new RedisTemplate<>();

		@Bean
		RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
			this.template.setConnectionFactory(connectionFactory);
			return this.template;
		}

	}

}
