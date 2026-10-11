**Commit**: 730272e396 · **Ticket**: KAFKA-18493
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    implementation libs.scalaLibrary
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18493: fix configure :streams:integration-tests project error (#18511).

**Test**
No test added.
