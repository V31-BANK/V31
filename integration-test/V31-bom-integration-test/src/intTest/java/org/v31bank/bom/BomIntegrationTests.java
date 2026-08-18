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

package org.v31bank.bom;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolves V31 from a repository the way a consumer does.
 * <p>
 * Every other test in this repository sees V31 as projects. A consumer sees coordinates
 * in a repository, and the two can disagree: the BOM names its artifacts by hand, so a
 * module that is published but never added to it resolves for this build and for nobody
 * else. Nothing else notices that, because nothing else asks by coordinate.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class BomIntegrationTests {

	/**
	 * Every artifact the BOM promises. Written out rather than derived, so that this
	 * fails when the BOM and the build disagree — deriving it from the same place the BOM
	 * comes from would make the two agree by construction and test nothing.
	 */
	private static final List<String> ARTIFACTS = List.of("V31-cbs-api", "V31-compliance-api", "V31-customer-api",
			"V31-ledger-api", "V31-notification-api", "V31-risk-api", "V31-transfer-api", "V31-wallet-api", "V31-core",
			"V31-data-jpa-spring-boot", "V31-data-valkey-spring-boot", "V31-grpc-spring-boot", "V31-jooq-spring-boot",
			"V31-web-spring-boot", "V31-data-jpa-spring-boot-starter", "V31-data-valkey-spring-boot-starter",
			"V31-grpc-spring-boot-starter", "V31-jooq-spring-boot-starter", "V31-web-spring-boot-starter");

	@TempDir
	private Path consumer;

	/**
	 * The whole point of the BOM: a consumer names an artifact without a version and gets
	 * one.
	 */
	@Test
	void resolvesEveryArtifactTheBomNamesWithoutAVersion() throws IOException {
		BuildResult result = new ConsumerBuild(this.consumer).resolve(ARTIFACTS);
		assertThat(result.getOutput()).contains("RESOLVED");
		for (String artifact : ARTIFACTS) {
			assertThat(result.getOutput()).as(artifact).contains(artifact + "-" + ConsumerBuild.VERSION + ".jar");
		}
	}

	/**
	 * The versions the platform decides have to survive publication, or a consumer gets
	 * an artifact it cannot use.
	 */
	@Test
	void resolvesTheThirdPartyLibrariesTheArtifactsNeed() throws IOException {
		BuildResult result = new ConsumerBuild(this.consumer).resolve(List.of("V31-data-jpa-spring-boot"));
		assertThat(result.getOutput()).contains("spring-boot-", "spring-data-jpa-", "hibernate-core-");
	}

	/**
	 * The internal platform is not published. A consumer importing the BOM must not end
	 * up needing it.
	 */
	@Test
	void needsNothingThatIsNotPublished() throws IOException {
		BuildResult result = new ConsumerBuild(this.consumer).resolve(ARTIFACTS);
		assertThat(result.getOutput()).doesNotContain("V31-internal-dependencies");
	}

}
