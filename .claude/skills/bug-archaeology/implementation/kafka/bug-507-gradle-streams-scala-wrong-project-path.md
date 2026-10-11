## Bug 507 — build.gradle: streams-scala subproject path wrong, caused Java 17 compile failure

**Commit:** c7800041c6
**Ticket:** (no JIRA – MINOR build fix)
**Subsystem:** build / gradle
**Category:** Wrong constant/default value

### Root cause
In `build.gradle` the `:streams-scala` project was listed as `:streams-scala` instead of `:streams:streams-scala`. Gradle resolved it as a top-level project which did not exist; this produced a silent no-op reference instead of a dependency on the real Kafka Streams Scala DSL module. As a result the Java 17 toolchain configuration and compile-time compatibility flags were not applied to the streams-scala subproject, causing build failures when Java 17 features were enabled.

### Before
```groovy
project(':streams-scala') {
    // Java 17 configuration never applied
}
```

### After
```groovy
project(':streams:streams-scala') {
    // correct path — Java 17 configuration applied
}
```

### How to spot
Multi-project Gradle builds where subprojects are nested (`:parent:child`) and the child is referenced without the parent prefix will silently fail to apply the block; the build may succeed but the configuration is quietly dropped.

### Test
Gradle build passes with Java 17 after the fix; CI compilation gate is the regression check.
