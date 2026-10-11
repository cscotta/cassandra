**Commit**: df5cc4a064 · **Ticket**: KAFKA-17073
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
            LOG.warn("This tool is deprecated and may be removed in a future major release.");

```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17073 deprecate replicaverificationtool (#16572).

**Test**
No test added.
