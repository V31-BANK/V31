@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "V31"

include("apis")
include("cloud")
include("module")
include("starter")
include("library")
include("platform")
include("processor")
include("integration-test")

include("library:V31-core")

include("apis:V31-customer-api")
include("apis:V31-wallet-api")
include("apis:V31-transfer-api")
include("apis:V31-ledger-api")
include("apis:V31-risk-api")
include("apis:V31-compliance-api")
include("apis:V31-cbs-api")
include("apis:V31-notification-api")

include("cloud:V31-customer-service")
include("cloud:V31-wallet-service")
include("cloud:V31-transfer-service")
include("cloud:V31-ledger-service")
include("cloud:V31-risk-service")
include("cloud:V31-compliance-service")
include("cloud:V31-cbs-service")
include("cloud:V31-notification-service")

include("platform:V31-dependencies")
include("platform:V31-internal-dependencies")

include("starter:V31-data-jpa-spring-boot-starter")
include("module:V31-data-jpa-spring-boot")
include("starter:V31-jooq-spring-boot-starter")
include("module:V31-jooq-spring-boot")
include("starter:V31-data-valkey-spring-boot-starter")
include("module:V31-data-valkey-spring-boot")
include("starter:V31-grpc-spring-boot-starter")
include("module:V31-grpc-spring-boot")
include("starter:V31-web-spring-boot-starter")
include("module:V31-web-spring-boot")

include("integration-test:V31-bom-integration-test")
