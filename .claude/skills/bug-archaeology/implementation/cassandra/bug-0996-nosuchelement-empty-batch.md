# Bug 0996: NoSuchElementException on Empty Batch Execution

**Commit**: 796db6e09c · **Ticket**: CASSANDRA-10711
**Subsystem**: CQL / batch statements
**Category**: Missing null / bounds check

---

## Root Cause
`BatchStatement.executeWithoutConditions` built a lazy `Iterable<PartitionUpdate>` by concatenating the updates from each mutation, then passed it to `StorageProxy.mutateAtomically` or `StorageProxy.mutate`. When the batch contained zero statements the mutations collection was empty. Calling `Iterables.concat` on an empty collection produced an empty iterable, but the downstream `StorageProxy` code called `.iterator().next()` unconditionally to read the first mutation's timestamp, throwing `NoSuchElementException`. The fix is an early return before building the iterable at all.

### Before / After
```java
// BEFORE — no guard, proceeds to build iterator over empty collection
private void executeWithoutConditions(Collection<? extends IMutation> mutations, ConsistencyLevel cl) {
    Iterable<PartitionUpdate> updates = Iterables.concat(
        Iterables.transform(mutations, ...));
    // downstream code throws NoSuchElementException on empty iterator

// AFTER — early return on empty input
private void executeWithoutConditions(Collection<? extends IMutation> mutations, ConsistencyLevel cl) {
    if (mutations.isEmpty())
        return;
    Iterable<PartitionUpdate> updates = Iterables.concat(...);
```

---

## How to Spot
Any method that builds a lazy iterable from a collection and then passes it to code that assumes at least one element is present is vulnerable to this pattern. Look for `Iterables.concat` / `Iterables.transform` chains where the source collection may legally be empty, and check that the consumer defends against that. Batch statements are a natural place for zero-length inputs since a client can send a syntactically valid but empty `BEGIN BATCH … APPLY BATCH`.

---

## Test
`BatchTests` is extended with a test that executes an empty `BEGIN BATCH APPLY BATCH` and verifies it completes without error.
