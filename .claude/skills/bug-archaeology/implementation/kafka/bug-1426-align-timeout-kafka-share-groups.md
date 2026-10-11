**Commit**: 85c337af44 · **Ticket**: KAFKA-18755
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            .defaultsTo(5000L);

// After
            .defaultsTo(30000L);
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18755 align timeout in kafka-share-groups.sh (#18908).

**Test**
No test added.
