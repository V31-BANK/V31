rootProject.name = "V31"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include("V31-cloud-platform")
include("framework-bom")
include("framework-platform")
include("V31-core")
include("V31-cloud-platform:V31-customer-service")
include("V31-cloud-platform:V31-customer-contract")
include("V31-cloud-platform:V31-wallet-service")
include("V31-cloud-platform:V31-wallet-contract")
include("V31-cloud-platform:V31-transfer-service")
include("V31-cloud-platform:V31-transfer-contract")
include("V31-cloud-platform:V31-ledger-service")
include("V31-cloud-platform:V31-ledger-contract")
include("V31-cloud-platform:V31-risk-service")
include("V31-cloud-platform:V31-risk-contract")
include("V31-cloud-platform:V31-compliance-service")
include("V31-cloud-platform:V31-compliance-contract")
include("V31-cloud-platform:V31-cbs-service")
include("V31-cloud-platform:V31-cbs-contract")
include("V31-cloud-platform:V31-notification-service")
include("V31-cloud-platform:V31-notification-contract")
