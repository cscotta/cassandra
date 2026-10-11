# Bug 1177: Fix Flapping PartitionTest

**Commit**: 2b0a8f6bda · **Ticket**: CASSANDRA-9731
**Subsystem**: src/java/org/apache/cassandra/db/RowUpdateBuilder.java
**Category**: Wrong constant / default value

---

## Root Cause

`RowUpdateBuilder.deleteRow` hard-coded the `localDeletionTime` of a row tombstone to `FBUtilities.nowInSeconds()` at the call site rather than accepting it as a parameter. In tests that need to assert a specific `minLocalDeletionTime` in `RowStats`, the tombstone's deletion time was always the real wall-clock value at the instant the test ran, making the assertion non-deterministic. The fix separates timestamp from `localDeletionTime` so callers can supply a known, stable value; a new `deleteRowAt` overload exposes this control while the existing `deleteRow` preserves its convenience signature.

### Before / After

```java
// BEFORE
private static void deleteRow(PartitionUpdate update, long timestamp, Object... clusteringValues) {
    builder.addRowDeletion(new DeletionTime(timestamp, FBUtilities.nowInSeconds()));
}
```

```java
// AFTER
private static void deleteRow(PartitionUpdate update, long timestamp, int localDeletionTime, Object... clusteringValues) {
    builder.addRowDeletion(new DeletionTime(timestamp, localDeletionTime));
}

public static Mutation deleteRowAt(CFMetaData metadata, long timestamp, int localDeletionTime, Object key, Object... clusteringValues) { ... }
```

---

## How to Spot

Tests that compare time-derived fields (timestamps, deletion times, TTL expirations) are fragile when the value is drawn from the live clock inside the production helper rather than being injected. Whenever a test asserts exact equality on a value that silently uses `nowInSeconds()` or similar, a flap is latent. Reviewers should look for `nowInSeconds()` calls inside shared builder/helper methods that have no corresponding parameter for tests to control.

---

## Test

`PartitionTest` is updated to call `deleteRowAt` with a fixed `localDeletionTime` and then assert that `RowStats.minLocalDeletionTime` equals exactly that value, eliminating the clock-dependent flap.
