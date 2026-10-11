**Commit**: 9d65ff8077 · **Ticket**: KAFKA-17852
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
      .help("When this option is passed, the format command will skip over already formatted directories rather than failing.")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17852 add help message to the --ignore-formatted flag of storagetool (#17577).

**Test**
No test added.
