**Commit**: c16c240bd1 · **Ticket**: KAFKA-18688
**Subsystem**: group-coordinator / UniformHomogeneousAssignmentBuilder
**Category**: Logic error in condition

**Root cause**
`UniformHomogeneousAssignmentBuilder.maybeRevokePartitions()` decremented `remainingMembersToGetAnExtraPartition` eagerly as soon as it decided a member "should" get the extra partition, before checking whether that member's prior assignment already satisfied the quota. This meant the count of members still needing an extra partition was consumed too early. When iterating over a group whose existing assignment was already balanced (with different members holding the extra partitions), the builder would unnecessarily reassign extra partitions to the first members in iteration order, breaking stability.

**Before/After**
```java
// Before — decrement happens immediately, disrupts already-balanced assignments
int quota = minimumMemberQuota;
if (remainingMembersToGetAnExtraPartition > 0) {
    quota++;
    remainingMembersToGetAnExtraPartition--;
}

// After — defer decrement; only take extra partition if this member must
int quota = minimumMemberQuota;
boolean quotaHasExtraPartition = false;
if (remainingMembersToGetAnExtraPartition > 0) {
    quota++;
    quotaHasExtraPartition = true;
}
// ... later, only decrement if this member keeps the extra partition
if (quotaHasExtraPartition) {
    remainingMembersToGetAnExtraPartition--;
}
```

**How to spot**
Any algorithm that allocates a limited pool of "bonus slots" by eagerly consuming them from a counter, rather than lazily confirming after the assignment step, risks consuming the pool in the wrong order. Check that the counter is decremented only when the member actually claims the extra item.

**Test**
`OptimizedUniformAssignmentBuilderTest.testReassignmentStickinessWhenAlreadyBalanced` — starts with a perfectly balanced assignment where members A and C hold the extra partitions, runs an incremental rebalance, and asserts the output matches the input exactly.
