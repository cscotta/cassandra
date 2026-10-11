**Commit**: 62206cfc54 · **Ticket**: KAFKA-20085
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
## Description

Replaces verbose manual version instructions with a new Gradle task
`updateVersion` and introduces a verification task
`verifyVersionConsistency` to enforce version consistency. **Features**

1. **Automated Verification (`check` lifecycle):**
The `verifyVersionConsistency` task is hooked into `./gradlew check`. If `gradle.properties` is updated but dependent files are not, the build
will **fail** with a descriptive error prompting the user to run
`updateVersion`.

**Before/After**
```java
// Before
// (no removals)

// After



// Helper to get version targets and expected patterns
ext.getVersionTargets = { rawVersion ->
    // Python Package Version (PEP 440): 4.3.0.dev0
    def pythonVersion = rawVersion.contains("SNAPSHOT") ?
            rawVersion.replace("-SNAPSHOT", ".dev0") :
            rawVersion

    def baseVersion = rawVersion.replace("-SNAPSHOT", "")

```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20085 replace verbose version instructions with updateversion task (#21340).

**Test**
No test added.
