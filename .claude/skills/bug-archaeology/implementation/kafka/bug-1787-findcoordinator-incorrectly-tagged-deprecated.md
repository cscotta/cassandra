**Commit**: 22d1ba8265 · **Ticket**: KAFKA-18270
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
It's now consistent with KIP-896.

**Before/After**
```java
// Before
  "deprecatedVersions": "0",

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18270: findcoordinator v0 incorrectly tagged as deprecated (#18262).

**Test**
No test added.
