**Commit**: 3d5d1504f7 · **Ticket**: KAFKA-16878
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  // Exclude PowerMock tests when running with Java 16 or newer until a version of PowerMock that supports the relevant versions is released
  // The relevant issues are https://github.com/powermock/powermock/issues/1094 and https://github.com/powermock/powermock/issues/1099
  if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
    testsToExclude.addAll([
      // connect tests
    ])
  }

      // NOTE: Jacoco Gradle plugin does not support "offline instrumentation" this means that classes mocked by PowerMock
      // may report 0 coverage, since the source was modified after initial instrumentation.
      // See https://github.com/jacoco/jacoco/issues/51
    testImplementation libs.easymock

// After


```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16878 remove powermock and easymock from code base (#16236).

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
