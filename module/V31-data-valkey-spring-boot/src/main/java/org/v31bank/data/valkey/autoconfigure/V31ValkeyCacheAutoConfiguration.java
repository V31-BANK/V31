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

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import org.v31bank.data.valkey.cache.ValkeyCacheErrorHandler;
import org.v31bank.data.valkey.cache.ValkeyCachingConfigurer;

/**
 * {@link AutoConfiguration Auto-configuration} for caching through Valkey: gives Spring's
 * cache abstraction the same JSON serialization the template uses, puts every cache under
 * this application's key prefix, and gives every cache an expiry.
 *
 * <h2>Why every cache expires</h2>
 *
 * Spring's default is no expiry at all. An entry written once then stays until something
 * evicts it, and an eviction missed — a write that went round the cache, a service that
 * crashed between updating the database and clearing the cache — is stale data that never
 * heals. A balance or a limit that is quietly wrong is worse than one that is briefly
 * missing, so entries are given a life and re-read when it ends.
 * {@code v31.data.valkey.cache.ttls} sets it per cache where ten minutes is the wrong
 * answer.
 * <p>
 * This only takes effect where the application has switched caching on with
 * {@code @EnableCaching}; putting the module on the classpath does not start caching
 * anything.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@AutoConfiguration(before = CacheAutoConfiguration.class, after = V31ValkeyAutoConfiguration.class)
@ConditionalOnClass({ RedisCacheManager.class, RedisCacheManagerBuilderCustomizer.class })
@ConditionalOnBooleanProperty(name = "v31.data.valkey.cache.enabled", matchIfMissing = true)
@EnableConfigurationProperties(V31ValkeyProperties.class)
public class V31ValkeyCacheAutoConfiguration {

	/**
	 * The configuration every cache starts from. Spring Boot picks this up and builds the
	 * cache manager on it.
	 * @param properties the prefix, the default expiry, and whether misses are cached
	 * @param valkeyValueSerializer the serializer values are written with
	 * @return the default cache configuration
	 */
	@Bean
	@ConditionalOnMissingBean
	public RedisCacheConfiguration valkeyCacheConfiguration(V31ValkeyProperties properties,
			@Qualifier("valkeyValueSerializer") RedisSerializer<Object> valkeyValueSerializer) {
		RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
			.prefixCacheNameWith(properties.getKeyPrefix() + ":cache:")
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valkeyValueSerializer));
		configuration = applyTtl(configuration, properties.getCache().getDefaultTtl());
		return properties.getCache().isAllowNullValues() ? configuration : configuration.disableCachingNullValues();
	}

	/**
	 * Apply the expiry configured for individual caches, on top of the defaults above.
	 * @param properties the per-cache expiries
	 * @param valkeyCacheConfiguration the configuration to vary
	 * @return the customizer
	 */
	@Bean
	public RedisCacheManagerBuilderCustomizer valkeyCacheTtlCustomizer(V31ValkeyProperties properties,
			RedisCacheConfiguration valkeyCacheConfiguration) {
		return (builder) -> properties.getCache()
			.getTtls()
			.forEach((cacheName, ttl) -> builder.withCacheConfiguration(cacheName,
					applyTtl(valkeyCacheConfiguration, ttl)));
	}

	/**
	 * Keep an unreachable Valkey from failing every cached call.
	 * <p>
	 * Backs off entirely when the application supplies its own {@link CachingConfigurer},
	 * since Spring accepts only one and the application's will be doing more than this.
	 * @param properties whether failures are allowed out
	 * @return the configurer carrying the error handler
	 */
	@Bean
	@ConditionalOnMissingBean(CachingConfigurer.class)
	public CachingConfigurer valkeyCachingConfigurer(V31ValkeyProperties properties) {
		return new ValkeyCachingConfigurer(new ValkeyCacheErrorHandler(properties.getCache().isFailFast()));
	}

	/**
	 * Set an expiry, leaving the configuration alone when the value is not a positive
	 * duration.
	 * <p>
	 * A zero or negative expiry is taken as a deliberate request for entries that never
	 * expire, which Spring's default already does. It is not corrected here — silently
	 * substituting a different lifetime for the one that was configured would be worse
	 * than honouring an unwise one.
	 * @param configuration the configuration to vary
	 * @param ttl how long an entry lives
	 * @return the configuration, with the expiry applied where there was one
	 */
	private static RedisCacheConfiguration applyTtl(RedisCacheConfiguration configuration, Duration ttl) {
		return (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? configuration.entryTtl(ttl) : configuration;
	}

}
