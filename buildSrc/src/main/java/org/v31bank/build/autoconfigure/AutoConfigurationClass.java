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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * An {@code @AutoConfiguration} class, read from its class file.
 *
 * @param name binary name of the class
 * @param references the classes it orders itself against
 * @author Xander Wang
 * @since 0.2.0
 */
public record AutoConfigurationClass(String name, List<Reference> references) {

	private static final ClassDesc AUTO_CONFIGURATION = ClassDesc
		.of("org.springframework.boot.autoconfigure.AutoConfiguration");

	private static final String CLASS_FILE_SUFFIX = ".class";

	/**
	 * Read a class file.
	 * @param classFile the file to read
	 * @return the class, or empty when it carries no {@code @AutoConfiguration}
	 */
	public static Optional<AutoConfigurationClass> of(Path classFile) {
		return of(parse(classFile));
	}

	/**
	 * Read a class from wherever it is held, which for a class inside a jar is not a
	 * file.
	 * @param input the bytecode to read
	 * @return the class, or empty when it carries no {@code @AutoConfiguration}
	 */
	public static Optional<AutoConfigurationClass> of(InputStream input) {
		return of(parse(input));
	}

	/**
	 * Whether anything outside the class's own package can name it, which is what decides
	 * whether it is worth telling the rest of the build about.
	 * @param classFile the file to read
	 * @return whether the class is public
	 */
	public static boolean isPublic(Path classFile) {
		return parse(classFile).flags().has(AccessFlag.PUBLIC);
	}

	/**
	 * Where a class of the given name was compiled to.
	 * @param className binary name of the class to look for
	 * @param classpath the roots to look under
	 * @return its class file, if one of the roots has it
	 */
	public static Optional<Path> classFileOf(String className, Iterable<File> classpath) {
		String relativePath = className.replace('.', '/') + CLASS_FILE_SUFFIX;
		for (File root : classpath) {
			Path classFile = root.toPath().resolve(relativePath);
			if (Files.isRegularFile(classFile)) {
				return Optional.of(classFile);
			}
		}
		return Optional.empty();
	}

	private static Optional<AutoConfigurationClass> of(ClassModel classModel) {
		// @AutoConfiguration is retained at runtime, so only the visible attribute can
		// hold it.
		return classModel.findAttribute(Attributes.runtimeVisibleAnnotations())
			.map(RuntimeVisibleAnnotationsAttribute::annotations)
			.orElse(List.of())
			.stream()
			.filter((annotation) -> AUTO_CONFIGURATION.equals(annotation.classSymbol()))
			.findFirst()
			.map((annotation) -> new AutoConfigurationClass(binaryNameOf(classModel.thisClass().asSymbol()),
					referencesIn(annotation)));
	}

	private static ClassModel parse(Path classFile) {
		try {
			return ClassFile.of().parse(classFile);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + classFile, ex);
		}
	}

	private static ClassModel parse(InputStream input) {
		try {
			return ClassFile.of().parse(input.readAllBytes());
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read a class", ex);
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

	public record Reference(Attribute attribute, String className) {
	}

	public enum Attribute {

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
