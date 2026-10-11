**Commit**: fa124385b8 · **Ticket**: KAFKA-17906
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  sourceSets {
    main {
      java {
        srcDirs = ["src/main/java"]
      }
    }
    test {
      java {
        srcDirs = ["src/test/java"]
      }
    }
  }

// After
// (no additions)
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17906 remove redundant sourcesets from build.gradle (#17672).

**Test**
No test added.
