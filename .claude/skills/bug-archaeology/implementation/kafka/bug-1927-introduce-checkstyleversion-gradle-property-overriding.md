**Commit**: f9215dae3b · **Ticket**: KAFKA-12770
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, Luke Chen <showuon@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  checkstyle: "8.36.2",

// After
For experiments (or regression testing purposes) add `-PcheckstyleVersion=X.y.z` switch (to override project-defined checkstyle version).
  checkstyle: project.hasProperty('checkstyleVersion') ? checkstyleVersion : "8.36.2",
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12770 introduce `checkstyleversion` gradle property (for overriding checkstyle project-defined dependency version).

**Test**
No test added.
