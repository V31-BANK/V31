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

package org.v31bank.core.util;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link Uuids}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class UuidsTests {

	@Test
	void generatesVersionSevenIdentifiers() {
		UUID uuid = Uuids.timeOrdered();
		assertThat(uuid.version()).isEqualTo(7);
		assertThat(uuid.variant()).isEqualTo(2);
	}

	@Test
	void ordersIdentifiersByWhenTheyWereIssued() {
		long previous = Long.MIN_VALUE;
		for (int i = 0; i < 20_000; i++) {
			long current = Uuids.timeOrdered().getMostSignificantBits();
			assertThat(current).as("identifier " + i + " did not advance").isGreaterThan(previous);
			previous = current;
		}
	}

	@Test
	void doesNotRepeatItselfUnderConcurrency() throws Exception {
		int threads = 8;
		int perThread = 5_000;
		try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
			List<Callable<Set<UUID>>> tasks = IntStream.range(0, threads)
				.<Callable<Set<UUID>>>mapToObj((i) -> () -> generate(perThread))
				.toList();
			Set<UUID> generated = new HashSet<>();
			for (Future<Set<UUID>> future : executor.invokeAll(tasks)) {
				generated.addAll(future.get());
			}
			assertThat(generated).hasSize(threads * perThread);
		}
	}

	@Test
	void carriesTheInstantItWasIssued() {
		Instant before = Instant.now().minusSeconds(1);
		Instant issued = Uuids.timestampOf(Uuids.timeOrdered());
		assertThat(issued).as(issued + " should be after " + before).isAfter(before);
		assertThat(issued).isBefore(Instant.now().plusSeconds(1));
	}

	@Test
	void refusesToReadATimestampThatIsNotThere() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Uuids.timestampOf(UUID.randomUUID()));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Uuids.timestampOf(null));
	}

	private static Set<UUID> generate(int count) {
		Set<UUID> generated = new HashSet<>(count);
		for (int i = 0; i < count; i++) {
			generated.add(Uuids.timeOrdered());
		}
		return generated;
	}

}
