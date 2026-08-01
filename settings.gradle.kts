@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "V31"

include("apis")
include("module")
include("starter")
include("library")
include("processor")
include("cloud-platform")
include("framework-bom")
include("framework-platform")

include("apis:V31-customer-api")
include("apis:V31-wallet-api")
include("apis:V31-transfer-api")
include("apis:V31-ledger-api")
include("apis:V31-risk-api")
include("apis:V31-compliance-api")
include("apis:V31-cbs-api")
include("apis:V31-notification-api")

include("cloud-platform:V31-customer-service")
include("cloud-platform:V31-wallet-service")
include("cloud-platform:V31-transfer-service")
include("cloud-platform:V31-ledger-service")
include("cloud-platform:V31-risk-service")
include("cloud-platform:V31-compliance-service")
include("cloud-platform:V31-cbs-service")
include("cloud-platform:V31-notification-service")

include("library:V31-core")
include("starter:V31-data-jpa-spring-boot-starter")
include("module:V31-data-jpa-spring-boot")
include("starter:V31-jooq-spring-boot-starter")
include("module:V31-jooq-spring-boot")
include("starter:V31-data-valkey-spring-boot-starter")
include("module:V31-data-valkey-spring-boot")
include("starter:V31-grpc-spring-boot-starter")
include("module:V31-grpc-spring-boot")
