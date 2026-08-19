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

package org.v31bank.build.autoconfigure;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.autoconfigure.AutoConfigurationClass.Attribute;
import org.v31bank.build.autoconfigure.AutoConfigurationClass.Reference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurationClass}.
 * <p>
 * Each test assembles a real class file and reads it back, so what is exercised is the
 * bytecode a compiler would have produced rather than a stand-in for it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class AutoConfigurationClassTests {

	@TempDir
	private Path classes;

	@Test
	void readsTheNameOfAnAnnotatedClass() {
		assertThat(read(ClassFiles.autoConfiguration("com.example.ExampleAutoConfiguration"))).get()
			.extracting(AutoConfigurationClass::name)
			.isEqualTo("com.example.ExampleAutoConfiguration");
	}

	@Test
	void readsANestedClassByItsBinaryName() {
		assertThat(read(ClassFiles.autoConfiguration("com.example.Outer$InnerAutoConfiguration"))).get()
			.extracting(AutoConfigurationClass::name)
			.isEqualTo("com.example.Outer$InnerAutoConfiguration");
	}

	@Test
	void readsNothingFromAClassThatCarriesNoAnnotation() {
		assertThat(read(ClassFiles.plainClass("com.example.Plain"))).isEmpty();
	}

	@Test
	void readsNothingFromAClassAnnotatedWithSomethingElse() {
		assertThat(read(ClassFiles.annotatedWith("com.example.Configured",
				"org.springframework.context.annotation.Configuration")))
			.isEmpty();
	}

	@Test
	void readsAClassValuedReferenceAsTheClassItNames() {
		assertThat(references(ClassFiles.autoConfiguration("com.example.ExampleAutoConfiguration")
			.before("com.example.OtherAutoConfiguration")))
			.containsExactly(new Reference(Attribute.BEFORE, "com.example.OtherAutoConfiguration"));
	}

	@Test
	void readsANameValuedReferenceAsTheNameItHolds() {
		assertThat(references(ClassFiles.autoConfiguration("com.example.ExampleAutoConfiguration")
			.beforeName("com.example.OtherAutoConfiguration")))
			.containsExactly(new Reference(Attribute.BEFORE_NAME, "com.example.OtherAutoConfiguration"));
	}

	@Test
	void readsEveryOrderingAttribute() {
		assertThat(references(ClassFiles.autoConfiguration("com.example.ExampleAutoConfiguration")
			.before("com.example.A")
			.beforeName("com.example.B")
			.after("com.example.C")
			.afterName("com.example.D"))).containsExactlyInAnyOrder(new Reference(Attribute.BEFORE, "com.example.A"),
					new Reference(Attribute.BEFORE_NAME, "com.example.B"),
					new Reference(Attribute.AFTER, "com.example.C"),
					new Reference(Attribute.AFTER_NAME, "com.example.D"));
	}

	@Test
	void readsEveryClassOneAttributeNames() {
		assertThat(references(ClassFiles.autoConfiguration("com.example.ExampleAutoConfiguration")
			.after("com.example.A", "com.example.B"))).containsExactly(new Reference(Attribute.AFTER, "com.example.A"),
					new Reference(Attribute.AFTER, "com.example.B"));
	}

	@Test
	void passesOverAnAttributeThatSaysNothingAboutOrdering() {
		assertThat(references(
				ClassFiles.autoConfiguration("com.example.ExampleAutoConfiguration").withSingleValued("value", "x")))
			.isEmpty();
	}

	@Test
	void passesOverAnOrderingAttributeThatHoldsNoArray() {
		assertThat(references(
				ClassFiles.autoConfiguration("com.example.ExampleAutoConfiguration").withSingleValued("before", "x")))
			.isEmpty();
	}

	@Test
	void pairsEachAttributeWithItsOtherForm() {
		assertThat(Attribute.BEFORE.counterpart()).isEqualTo(Attribute.BEFORE_NAME);
		assertThat(Attribute.BEFORE_NAME.counterpart()).isEqualTo(Attribute.BEFORE);
		assertThat(Attribute.AFTER.counterpart()).isEqualTo(Attribute.AFTER_NAME);
		assertThat(Attribute.AFTER_NAME.counterpart()).isEqualTo(Attribute.AFTER);
	}

	@Test
	void knowsWhichFormRefersToItsTargetByName() {
		assertThat(Attribute.BEFORE.refersByName()).isFalse();
		assertThat(Attribute.AFTER.refersByName()).isFalse();
		assertThat(Attribute.BEFORE_NAME.refersByName()).isTrue();
		assertThat(Attribute.AFTER_NAME.refersByName()).isTrue();
	}

	@Test
	void findsAnAttributeByTheNameItHasInTheAnnotation() {
		assertThat(Attribute.of("beforeName")).contains(Attribute.BEFORE_NAME);
		assertThat(Attribute.of("conditionEvaluationReport")).isEmpty();
	}

	private Optional<AutoConfigurationClass> read(ClassFiles.Builder classFile) {
		return AutoConfigurationClass.of(classFile.writeTo(this.classes));
	}

	private Iterable<Reference> references(ClassFiles.Builder classFile) {
		return read(classFile).orElseThrow().references();
	}

}
