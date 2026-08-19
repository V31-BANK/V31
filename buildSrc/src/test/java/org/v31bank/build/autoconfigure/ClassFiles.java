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
import java.lang.classfile.ClassFile;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Class files assembled for a test to read.
 * <p>
 * What these checks read is bytecode, so what a test hands them has to be bytecode.
 * Assembling it here rather than compiling a fixture keeps Spring Boot off the test
 * classpath and lets a test say what the class declares instead of how a class file is
 * laid out.
 *
 * @author Xander Wang
 */
final class ClassFiles {

	private static final String AUTO_CONFIGURATION = "org.springframework.boot.autoconfigure.AutoConfiguration";

	private ClassFiles() {
	}

	/**
	 * A class carrying {@code @AutoConfiguration}.
	 * @param className binary name of the class to write
	 * @return a builder for it
	 */
	static Builder autoConfiguration(String className) {
		return new Builder(className, AUTO_CONFIGURATION);
	}

	/**
	 * A class carrying some other annotation, which the reader has to pass over.
	 * @param className binary name of the class to write
	 * @param annotationClassName binary name of the annotation it carries
	 * @return a builder for it
	 */
	static Builder annotatedWith(String className, String annotationClassName) {
		return new Builder(className, annotationClassName);
	}

	/**
	 * A class carrying no annotation at all.
	 * @param className binary name of the class to write
	 * @return a builder for it
	 */
	static Builder plainClass(String className) {
		return new Builder(className, null);
	}

	/**
	 * A class file waiting to be told what it declares.
	 */
	static final class Builder {

		private final String className;

		private final String annotationClassName;

		private final List<AnnotationElement> elements = new ArrayList<>();

		private boolean isPublic = true;

		private Builder(String className, String annotationClassName) {
			this.className = className;
			this.annotationClassName = annotationClassName;
		}

		Builder before(String... classNames) {
			return withElement("before", classNames, (name) -> AnnotationValue.ofClass(ClassDesc.of(name)));
		}

		Builder after(String... classNames) {
			return withElement("after", classNames, (name) -> AnnotationValue.ofClass(ClassDesc.of(name)));
		}

		Builder beforeName(String... classNames) {
			return withElement("beforeName", classNames, AnnotationValue::ofString);
		}

		Builder afterName(String... classNames) {
			return withElement("afterName", classNames, AnnotationValue::ofString);
		}

		/**
		 * A class nothing outside its own package can name.
		 * @return this builder
		 */
		Builder notPublic() {
			this.isPublic = false;
			return this;
		}

		/**
		 * An attribute holding one value rather than the array both real forms hold.
		 * @param name the attribute to add
		 * @param value its value
		 * @return this builder
		 */
		Builder withSingleValued(String name, String value) {
			this.elements.add(AnnotationElement.of(name, AnnotationValue.ofString(value)));
			return this;
		}

		private Builder withElement(String name, String[] classNames, Function<String, AnnotationValue> asValue) {
			AnnotationValue[] values = Arrays.stream(classNames).map(asValue).toArray(AnnotationValue[]::new);
			this.elements.add(AnnotationElement.of(name, AnnotationValue.ofArray(values)));
			return this;
		}

		/**
		 * Write the class file where a classpath rooted at the given directory would
		 * expect to find it.
		 * @param classesDirectory the root to write under
		 * @return the file written
		 */
		Path writeTo(Path classesDirectory) {
			byte[] bytes = ClassFile.of().build(ClassDesc.of(this.className), (builder) -> {
				builder.withFlags(this.isPublic ? new AccessFlag[] { AccessFlag.PUBLIC } : new AccessFlag[0]);
				if (this.annotationClassName != null) {
					builder.with(RuntimeVisibleAnnotationsAttribute.of(Annotation
						.of(ClassDesc.of(this.annotationClassName), this.elements.toArray(AnnotationElement[]::new))));
				}
			});
			Path classFile = classesDirectory.resolve(this.className.replace('.', '/') + ".class");
			try {
				Files.createDirectories(classFile.getParent());
				Files.write(classFile, bytes);
				return classFile;
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to write " + classFile, ex);
			}
		}

	}

}
