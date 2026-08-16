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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import org.v31bank.data.valkey.lock.ValkeyLock;
import org.v31bank.data.valkey.util.ValkeyKeys;

/**
 * {@link AutoConfiguration Auto-configuration} for V31 Data Valkey support: replaces the
 * template Spring Boot would otherwise supply with one that stores JSON under readable
 * keys, and registers the key builder and the lock built on top of it.
 *
 * <h2>Why the template is replaced rather than added to</h2>
 *
 * Spring Boot's {@code redisTemplate} serialises with JDK serialization. That writes a
 * binary blob nobody can read from a console, breaks the moment a cached class gains or
 * loses a field, and — the reason it matters here — turns whatever is in Valkey into
 * instructions: reading an entry an attacker was able to write reconstructs arbitrary
 * objects, and that is a remote code execution, not a deserialization error. Leaving that
 * bean in the context as the obvious one to inject would be leaving a trap, so this takes
 * its name.
 *
 * <h2>Why the types are restricted</h2>
 *
 * JSON on its own cannot say which class an entry should come back as, so the serializer
 * records the type alongside the value. Honouring that type without checking it has the
 * same problem as JDK serialization, one format removed — which is why Spring Data's own
 * switch for it is called {@code enableUnsafeDefaultTyping}. This configures the checked
 * form instead: an entry may only name a type under
 * {@code v31.data.valkey.serialization.trusted-packages}, which starts at this platform's
 * own packages and the collections its values are wrapped in.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@AutoConfiguration(before = DataRedisAutoConfiguration.class)
@ConditionalOnClass({ RedisConnectionFactory.class, GenericJacksonJsonRedisSerializer.class })
@EnableConfigurationProperties(V31ValkeyProperties.class)
public class V31ValkeyAutoConfiguration {

	/**
	 * The serializer used for values, everywhere: by the template below and by the cache
	 * configuration, so that an entry written through one is readable through the other.
	 * @param properties the types an entry is allowed to name
	 * @return the serializer
	 */
	@Bean
	@ConditionalOnMissingBean(name = "valkeyValueSerializer")
	public RedisSerializer<Object> valkeyValueSerializer(V31ValkeyProperties properties) {
		return GenericJacksonJsonRedisSerializer.builder()
			.enableDefaultTyping(trustedTypes(properties))
			.enableSpringCacheNullValueSupport()
			.build();
	}

	/**
	 * Takes the name Spring Boot's own template would have used, so that code injecting
	 * {@code RedisTemplate} gets the safe one without knowing to ask for it. Boot's
	 * definition backs off when this one is registered first, which is what
	 * {@code before = DataRedisAutoConfiguration.class} arranges.
	 * @param connectionFactory the connection to Valkey
	 * @param valkeyValueSerializer the serializer for values
	 * @return the template
	 */
	@Bean
	@ConditionalOnMissingBean(name = "redisTemplate")
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
			@Qualifier("valkeyValueSerializer") RedisSerializer<Object> valkeyValueSerializer) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(RedisSerializer.string());
		template.setHashKeySerializer(RedisSerializer.string());
		template.setValueSerializer(valkeyValueSerializer);
		template.setHashValueSerializer(valkeyValueSerializer);
		return template;
	}

	@Bean
	@ConditionalOnMissingBean
	public ValkeyKeys valkeyKeys(V31ValkeyProperties properties) {
		return new ValkeyKeys(properties.getKeyPrefix());
	}

	@Bean
	@ConditionalOnMissingBean
	public ValkeyLock valkeyLock(StringRedisTemplate stringRedisTemplate) {
		return new ValkeyLock(stringRedisTemplate);
	}

	/**
	 * Build the check applied to the type an entry names when it is read back.
	 * <p>
	 * Matching is by package prefix, so a package listed here covers everything under it.
	 * Listing {@code java} would readmit every class the runtime ships with and undo the
	 * point of checking at all.
	 * @param properties the trusted package prefixes
	 * @return the validator
	 */
	private static PolymorphicTypeValidator trustedTypes(V31ValkeyProperties properties) {
		BasicPolymorphicTypeValidator.Builder validator = BasicPolymorphicTypeValidator.builder();
		for (String trustedPackage : properties.getSerialization().getTrustedPackages()) {
			validator.allowIfSubType(trustedPackage);
		}
		return validator.build();
	}

}
