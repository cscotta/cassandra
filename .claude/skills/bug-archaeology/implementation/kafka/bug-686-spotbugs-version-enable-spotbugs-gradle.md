**Commit**: e00be577c7 · **Ticket**: KAFKA-19771
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Prologue:
- https://github.com/apache/kafka/pull/20561#issuecomment-3380364683
___

Related JIRA ticket: KAFKA-19771

details:

- spotbugs: 4.9.4 -->> 4.9.8
- spotbugs gradle plugin: 6.2.5 -->> 6.4.4
- spotbugs tasks are enabled for Java 25

related links:
-

https://github.com/spotbugs/spotbugs/blob/4.9.8/CHANGELOG.md#498---2025-10-18
- https://github.com/spotbugs/spotbugs-gradle-plugin/releases/tag/6.4.4

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
          java-version: 17
  id "com.github.spotbugs" version '6.2.5' apply false
  if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_25)) {
    // Spotbugs is not compatible with Java 25+ so Gradle related tasks are disabled
    // until version can be upgraded: https://github.com/spotbugs/spotbugs/issues/3564
    project.gradle.startParameter.excludedTaskNames.add("spotbugsMain")
    project.gradle.startParameter.excludedTaskNames.add("spotbugsTest")
  }

  spotbugs: "4.9.4",

// After
          java-version: 25
  id "com.github.spotbugs" version '6.4.4' apply false
  spotbugs: "4.9.8",
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19771 update spotbugs version and enable spotbugs gradle tasks on java 25 (#20704).

**Test**
No test added.
