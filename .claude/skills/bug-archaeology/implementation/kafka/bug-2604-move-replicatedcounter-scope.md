**Commit**: c9d415f361 · **Ticket**: KAFKA-17275
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
// (no additions)
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17275: move replicatedcounter to test scope (#16814).

**Test**
Test modified in ReplicatedCounter.java covering the corrected behaviour.
