
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "V31"

include("module")
include("starter")
include("library")
include("processor")
include("cloud-platform")
include("framework-bom")
include("framework-platform")
include("library:V31-core")
include("cloud-platform:V31-customer-service")
include("cloud-platform:V31-customer-contract")
include("cloud-platform:V31-wallet-service")
include("cloud-platform:V31-wallet-contract")
include("cloud-platform:V31-transfer-service")
include("cloud-platform:V31-transfer-contract")
include("cloud-platform:V31-ledger-service")
include("cloud-platform:V31-ledger-contract")
include("cloud-platform:V31-risk-service")
include("cloud-platform:V31-risk-contract")
include("cloud-platform:V31-compliance-service")
include("cloud-platform:V31-compliance-contract")
include("cloud-platform:V31-cbs-service")
include("cloud-platform:V31-cbs-contract")
include("cloud-platform:V31-notification-service")
include("cloud-platform:V31-notification-contract")
