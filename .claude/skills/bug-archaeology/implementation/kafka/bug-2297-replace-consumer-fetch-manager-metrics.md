**Commit**: cfd7d94108 · **Ticket**: KAFKA-17657
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                "preferred-read-replica", "consumer-fetch-manager-metrics",

// After
                "preferred-read-replica", groupName,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17657 replace the consumer-fetch-manager-metrics by groupname (#17317).

**Test**
No test added.
