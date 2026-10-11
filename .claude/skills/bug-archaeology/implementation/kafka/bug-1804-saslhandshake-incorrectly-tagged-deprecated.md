**Commit**: cd5a7ee6b2 · **Ticket**: KAFKA-18270
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
It's now consistent with KIP-896. Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
  "deprecatedVersions": "0",

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18270: saslhandshake v0 incorrectly tagged as deprecated (#18221).

**Test**
No test added.
