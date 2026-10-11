**Commit**: 42f267a853 · **Ticket**: KAFKA-16944
**Subsystem**: group-coordinator/group-coordinator-api
**Category**: Logic error in condition

**Root cause**
The server side range assignor was made to be sticky i.e. partitions from the existing assignment are retained as much as possible. During a rebalance, the expected behavior is to achieve co-partitioning for members that are subscribed to the same set of topics with equal number of partitions. However, there are cases where this cannot be achieved efficiently with the current algorithm.

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * Gets the instance Id if present.
     *
     * @return An Optional containing the instance Id, or an empty Optional if not present.
     */
    Optional<String> instanceId();

```

**How to spot**
When reviewing group-coordinator/group-coordinator-api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16944; rewrite range assignor (#16504).

**Test**
Test modified in GroupSpecImplTest.java covering the corrected behaviour.
