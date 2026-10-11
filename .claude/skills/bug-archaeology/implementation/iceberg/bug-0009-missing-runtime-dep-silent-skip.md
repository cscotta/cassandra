# Bug 0009: Flink histogram metrics silently broken after optional dependency removed from runtime jar

**Commit**: dd45bd926bed9bfdca28aa1221d05d39e3dcfd59 · **Ticket**: #16126

## Detection Rule

When a dependency is moved from `implementation` to `compileOnly` in a fat/shadow runtime jar, verify that the runtime jar's shadow configuration still bundles the dependency if the feature it enables is expected to work without user action.

## Code Smell

A library is listed as `compileOnly` in the main module (so it compiles) but is absent from the `implementation` block of the corresponding `-runtime` shadow-jar subproject, leaving the class absent from the fat jar at runtime.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-bundled-optional-dep
- severity: silent-wrong-result
- subsystem: flink/runtime-jar

## Root Cause

Iceberg's Flink integration uses Dropwizard Metrics for Histogram implementations. Flink itself marks `flink-metrics-dropwizard` as optional and does not ship it in its distribution. A previous change (#16093) removed the dependency from the runtime jar, so any Histogram metric call would silently fail (no histogram data recorded) for users of the `iceberg-flink-runtime` fat jar who had not manually added the dependency. The fix adds `flink-metrics-dropwizard` back as an `implementation` dependency of the runtime jar project so it is bundled in the shade.

## Example

```groovy
// BEFORE: flink-metrics-dropwizard present only as compileOnly in main module;
// absent from runtime jar's dependencies
project(":iceberg-flink:iceberg-flink-runtime-${flinkMajorVersion}") {
    // ... no flink-metrics-dropwizard entry
    shadowJar { ... }
}
```

```groovy
// AFTER: explicitly bundled in the runtime (fat) jar
project(":iceberg-flink:iceberg-flink-runtime-${flinkMajorVersion}") {
    // To support dropwizard histogram metrics (not shipped by Flink by default)
    implementation libs.flink21.metrics.dropwizard
    shadowJar { ... }
}
```
