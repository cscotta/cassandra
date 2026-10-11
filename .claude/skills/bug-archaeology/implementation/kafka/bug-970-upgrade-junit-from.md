**Commit**: 465b01cd2c · **Ticket**: KAFKA-19382
**Subsystem**: build.gradle
**Category**: Resource leak

**Root cause**
jira: https://issues.apache.org/jira/browse/KAFKA-19382

Upgrade junit from 5.10.2 to
[5.13.1](https://github.com/junit-team/junit5/releases). A new behavior was introduced to junit 5.12

(https://github.com/junit-team/junit5/commit/89a46dfa10c6447ef010fbff7903bfcb3c18975a),
disallowing `ClusterTestExtensions` to generate empty invocation
contexts. However, `ClusterTestExtensions` is invoked by junit extension
so it could result in empty contexts for some tests. ```
> Configure project :
Starting build with version 4.1.0-SNAPSHOT (commit id c4a769bc) using
Gradle 8.14.1, Java 17 and Scala 2.13.16
Build properties: ignoreFailures=false, maxParallelForks=10,
maxScalacThreads=8, maxTestRetries=...

**Before/After**
```java
// Before
// (no removals)

// After
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
```

**How to spot**
When reviewing build.gradle code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19382:upgrade junit from 5.10 to 5.13 (#19919).

**Test**
Test modified in ClusterTestExtensions.java covering the corrected behaviour.
