**Commit**: 4a8ad8ec63 · **Ticket**: KAFKA-19781
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java
**Category**: Logic error in condition

**Root cause**
The `shouldInitialize()` method in `SubscriptionState` returned `true` for any partition in `INITIALIZING` state, including partitions that had already been marked for revocation via `pendingRevocation`. Initializing a fetch position for a partition being revoked is wasted work, and worse, it could throw `NoOffsetForPartitionException` on a partition the application already gave up. The fetch path already had a guard (`isFetchable()` checks `pendingRevocation`), but `shouldInitialize()` did not, making the two paths inconsistent.

**Before/After**
```java
// Before
private boolean shouldInitialize() {
    return fetchState.equals(FetchStates.INITIALIZING);
}

// After
private boolean shouldInitialize() {
    return fetchState.equals(FetchStates.INITIALIZING) && !pendingRevocation;
}
```

**How to spot**
When a flag like `pendingRevocation` is introduced to suppress one operation (fetching), review every other operation that checks the same state machine (`INITIALIZING`, `FETCHABLE`, etc.) to confirm the flag is applied consistently. A quick grep for all uses of `FetchStates.INITIALIZING` would have shown the asymmetry between `isFetchable()` and `shouldInitialize()`.

**Test**
`testMarkingPendingRevocationPreventsInitializingPosition` assigns `tp0`, confirms it appears in `initializingPartitions()`, then calls `markPendingRevocation` and asserts the partition is no longer present in `initializingPartitions()`.
