# Build Logic

The build's own source code. Gradle compiles this before anything else and puts the
result on the classpath of every build script in the repository, so what is written
here is available to the root build and to all of its subprojects without any of them
declaring a dependency on it.

Nothing here ends up in a service. It is build-time code only: never on an
application's classpath, and never published.

## Why it is a project rather than script

A convention repeated in twenty build files drifts in twenty directions. Moving it
here makes it one thing: it compiles, so a mistake is a compile error rather than a
failure halfway through someone's build; it can be read in one place; and it can be
tested like any other code.

The rule of thumb is that a rule belonging to more than one project belongs here, and
a rule belonging to exactly one project belongs in that project's build file.

## How it reaches the rest of the build

Plugins are registered by id in `build.gradle`:

```groovy
gradlePlugin {
    plugins {
        conventionsPlugin {
            id = "org.v31bank.conventions"
            implementationClass = "org.v31bank.build.ConventionsPlugin"
        }
    }
}
```

That generates `META-INF/gradle-plugins/<id>.properties` inside the jar, which is what
`apply(plugin = "...")` resolves against. The root build applies the conventions plugin
to every subproject, and the plugin decides for itself what each one needs by reacting
to the plugins that project has — so a `java-platform`, a plain library and a Spring
Boot application can all be handed the same treatment without being special-cased.

Tasks are ordinary classes; a project registers one where it needs it.

## It is a separate build

Two consequences that surprise people, both of them deliberate:

**The main build's `gradle.properties` does not reach it.** `settings.gradle` loads
that file by hand so a version can be declared once and used by both. Without it, every
version would have to be written twice and the two copies would drift.

## Keep it cheap

Every Gradle invocation in this repository builds this project first, so its build time
is added to everything. Prefer a small number of dependencies, and prefer reacting to
plugins over eager configuration.
