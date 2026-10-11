**Commit**: c162d2eb14 · **Ticket**: KAFKA-19522

**Subsystem**: KRaft / PartitionChangeBuilder

**Category**: Logic error in condition

**Root cause**
`PartitionChangeBuilder` contained an inverted boolean condition when evaluating whether a candidate could serve as the "fenced last known leader." The guard was `if (isAcceptableLeader.test(replica))` but the intended semantics were to skip replicas that ARE acceptable as regular leaders and only fall through to the fenced-leader path for replicas that are NOT acceptable. Inverting the condition to `if (!isAcceptableLeader.test(replica))` restores the correct election priority.

**Before/After**
```java
// Before: inverted predicate allows wrong replicas as fenced leader
if (isAcceptableLeader.test(replica)) {
    fencedLastKnownLeader = replica;
}

// After: correct guard — only non-acceptable replicas considered
if (!isAcceptableLeader.test(replica)) {
    fencedLastKnownLeader = replica;
}
```

**How to spot**
Negation bugs in predicate guards are best found by tracing the expected logical flow: write out what the condition *should* permit and compare to what the code actually tests. Unit tests that verify election outcomes for fenced-but-last-known replicas would expose this immediately.

**Test**
`PartitionChangeBuilderTest` adds a scenario with a fenced last-known leader and verifies it is selected only when no unfenced acceptable leader exists.
