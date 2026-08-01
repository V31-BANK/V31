import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.id

// Taken from Maven Central by coordinate rather than declared in a `plugins`
// block, because the plugin marker that block resolves lives only on the Gradle
// Plugin Portal. This keeps the build on the single repository the rest of the
// project is locked to.
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.5")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.google.protobuf")

    dependencies {
        // Exported, not implementation-only: the generated messages and stubs are
        // the contract, so anything depending on an API project needs these types
        // on its own compile classpath to name them.
        add("api", "com.google.protobuf:protobuf-java")
        add("api", "io.grpc:grpc-protobuf")
        add("api", "io.grpc:grpc-stub")

        // The generated stubs carry @javax.annotation.Generated, which left the
        // JDK in 11. Needed to compile them, never at runtime.
        add("compileOnly", "javax.annotation:javax.annotation-api:1.3.2")
    }

    configure<ProtobufExtension> {
        // Pinned to the versions the platform already resolves, so the compiler
        // that generates the code and the libraries that run it cannot drift.
        protoc {
            artifact = "com.google.protobuf:protoc:4.34.2"
        }
        plugins {
            id("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:1.81.0"
            }
        }
        generateProtoTasks {
            all().forEach { task ->
                task.plugins {
                    id("grpc")
                }
            }
        }
    }
}
