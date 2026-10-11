**Commit**: 9ac102596b · **Ticket**: KAFKA-16971
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    "Failed to parse host name from {} for the configuration {}. Each " +
                    "Failed to parse host port from {} for the configuration {}. Each " +

// After
                    "Failed to parse host name from %s for the configuration %s. Each " +
                    "Failed to parse host port from %s for the configuration %s. Each " +
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16971 fix the incorrect format string in quorumconfigs#parsebootstrapserver (#16358).

**Test**
No test added.
