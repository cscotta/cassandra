**Commit**: e0f35e025a · **Ticket**: KAFKA-18278
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
name: "Gradle Setup"
description: "Setup Java and Gradle"

// After
name: "Run Tests with Gradle"
description: "Run specified Gradle test tasks with configuration for timeout and test catalog."
```

**How to spot**
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18278: correct name and description for run-gradle step (#18223).

**Test**
No test added.
