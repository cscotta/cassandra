# Bug 0123: Google Cloud dependencies declared as implementation leak into runtime JARs

**Commit**: ff298a676d287f6bb065c030aca2ef66bbab4fed · **Ticket**: #15655

## Detection Rule

When a Gradle subproject declares third-party cloud-SDK dependencies as `implementation` rather than `compileOnly`, verify that those dependencies are not expected to be provided by the calling environment at runtime and that they do not get bundled/leaked into the project's published JAR.

## Code Smell

A `build.gradle` block where `platform(...)` BOM and downstream cloud-library artifacts (e.g. `google-cloud-bigquery`, `google-cloud-core`) are listed under `implementation` for a module that is consumed as a library, without a matching `shadow` or `runtimeOnly` exclusion.

## Tags

- scope: universal
- category: resource-leak
- code-pattern: wrong-gradle-scope-leaks-dep
- severity: silent-wrong-result
- subsystem: build / iceberg-bigquery

## Root Cause

The `iceberg-bigquery` module declared `google-cloud-bigquery`, `google-cloud-core`, and their BOM as `implementation` dependencies. In Gradle, `implementation` places artifacts on the compile classpath AND the runtime classpath, so they are included in the resolved runtime configuration and end up bundled in (or alongside) published JARs. The correct scope is `compileOnly` because the Google Cloud libraries are provided by the end user's environment and should not be shipped inside the Iceberg artifact.

## Example

```groovy
// BEFORE
implementation platform(libs.google.libraries.bom)
compileOnly "com.google.cloud:google-cloud-storage"
implementation "com.google.cloud:google-cloud-bigquery"
implementation "com.google.cloud:google-cloud-core"
```

```groovy
// AFTER
compileOnly platform(libs.google.libraries.bom)
compileOnly "com.google.cloud:google-cloud-storage"
compileOnly "com.google.cloud:google-cloud-bigquery"
compileOnly "com.google.cloud:google-cloud-core"
```
