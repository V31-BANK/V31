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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * One {@code @AutoConfiguration} class, read from the class file the compiler produced.
 * <p>
 * Reading the bytecode rather than the source is what keeps the checks in this package
 * honest: they see the annotation exactly as Spring Boot will see it at runtime, and they
 * see it for every class in the source set rather than for the ones somebody remembered
 * to list. The JDK reads class files itself, so this costs {@code buildSrc} no
 * dependency.
 *
 * @param name binary name of the class
 * @param references the classes it orders itself against
 * @author Xander Wang
 * @since 0.2.0
 */
record AutoConfigurationClass(String name, List<Reference> references) {

	private static final ClassDesc AUTO_CONFIGURATION = ClassDesc
		.of("org.springframework.boot.autoconfigure.AutoConfiguration");

	/**
	 * Read a class file.
	 * @param classFile the file to read
	 * @return the class, or empty when it carries no {@code @AutoConfiguration}
	 */
	static Optional<AutoConfigurationClass> of(Path classFile) {
		ClassModel classModel = parse(classFile);
		// @AutoConfiguration is retained at runtime, so the visible attribute is the only
		// one that can be holding it.
		return classModel.findAttribute(Attributes.runtimeVisibleAnnotations())
			.map(RuntimeVisibleAnnotationsAttribute::annotations)
			.orElse(List.of())
			.stream()
			.filter((annotation) -> AUTO_CONFIGURATION.equals(annotation.classSymbol()))
			.findFirst()
			.map((annotation) -> new AutoConfigurationClass(binaryNameOf(classModel.thisClass().asSymbol()),
					referencesIn(annotation)));
	}

	/**
	 * Whether anything outside the class's own package can name it, which is what decides
	 * whether it is worth telling the rest of the build about.
	 * @param classFile the file to read
	 * @return whether the class is public
	 */
	static boolean isPublic(Path classFile) {
		return parse(classFile).flags().has(AccessFlag.PUBLIC);
	}

	private static ClassModel parse(Path classFile) {
		try {
			return ClassFile.of().parse(classFile);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + classFile, ex);
		}
	}

	private static List<Reference> referencesIn(Annotation annotation) {
		List<Reference> references = new ArrayList<>();
		for (AnnotationElement element : annotation.elements()) {
			Attribute.of(element.name().stringValue())
				.ifPresent((attribute) -> targetsOf(element.value())
					.forEach((target) -> references.add(new Reference(attribute, target))));
		}
		return List.copyOf(references);
	}

	/**
	 * The classes one attribute points at. Both forms hold an array; only what the array
	 * holds differs, a class in one and that class's name in the other.
	 * @param value the value of the attribute
	 * @return the binary names it points at
	 */
	private static List<String> targetsOf(AnnotationValue value) {
		if (!(value instanceof AnnotationValue.OfArray array)) {
			return List.of();
		}
		return array.values().stream().map(AutoConfigurationClass::targetOf).flatMap(Optional::stream).toList();
	}

	private static Optional<String> targetOf(AnnotationValue element) {
		return switch (element) {
			case AnnotationValue.OfClass ofClass -> Optional.of(binaryNameOf(ofClass.classSymbol()));
			case AnnotationValue.OfString ofString -> Optional.of(ofString.stringValue());
			default -> Optional.empty();
		};
	}

	private static String binaryNameOf(ClassDesc type) {
		String packageName = type.packageName();
		return packageName.isEmpty() ? type.displayName() : packageName + "." + type.displayName();
	}

	/**
	 * One class an auto-configuration orders itself against, and which attribute said so.
	 *
	 * @param attribute the attribute it was declared in
	 * @param className binary name of the class pointed at
	 */
	record Reference(Attribute attribute, String className) {
	}

	/**
	 * The four attributes {@code @AutoConfiguration} offers for ordering, which are two
	 * attributes in two forms: one takes the class, the other its name.
	 * <p>
	 * Which form is correct is not a matter of taste. Naming a class loads it when the
	 * annotation is read, so a class from a dependency that may be absent has to be
	 * referred to by name; a class that is always there referred to by name is a string
	 * nothing checks.
	 */
	enum Attribute {

		/**
		 * {@code before}, holding classes.
		 */
		BEFORE("before"),

		/**
		 * {@code beforeName}, holding the same classes by name.
		 */
		BEFORE_NAME("beforeName"),

		/**
		 * {@code after}, holding classes.
		 */
		AFTER("after"),

		/**
		 * {@code afterName}, holding the same classes by name.
		 */
		AFTER_NAME("afterName");

		private final String attributeName;

		Attribute(String attributeName) {
			this.attributeName = attributeName;
		}

		String attributeName() {
			return this.attributeName;
		}

		boolean refersByName() {
			return this == BEFORE_NAME || this == AFTER_NAME;
		}

		/**
		 * The same attribute in the other form, which is what a wrongly declared
		 * reference should have used.
		 * @return the counterpart attribute
		 */
		Attribute counterpart() {
			return switch (this) {
				case BEFORE -> BEFORE_NAME;
				case BEFORE_NAME -> BEFORE;
				case AFTER -> AFTER_NAME;
				case AFTER_NAME -> AFTER;
			};
		}

		static Optional<Attribute> of(String attributeName) {
			return Arrays.stream(values())
				.filter((attribute) -> attribute.attributeName.equals(attributeName))
				.findFirst();
		}

	}

}
