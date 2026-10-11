# Bug 0669: Counter Cell Digest Includes Raw Encoded Bytes Instead of Logical Value

**Commit**: eb6f03c892 · **Ticket**: CASSANDRA-13750
**Subsystem**: src/java/org/apache/cassandra/db/rows/AbstractCell.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`AbstractCell.digest()` fed the raw byte buffer of a cell's value directly into the `MessageDigest`. For counter cells the value is a `CounterContext` — a binary encoding that contains local shard identifiers which differ per replica. Two replicas holding the same logical counter value would produce different raw bytes (because the context shards carry node-local IDs), and thus different digests, causing spurious read-repair digest mismatches. The fix delegates counter-cell digesting to `CounterContext.instance().updateDigest()`, which already normalizes the context to a canonical form before hashing, and which was already used by the existing test — but was not wired through the general `digest()` path.

### Before / After

```java
// BEFORE — raw bytes hashed, differs per replica for counters
public void digest(MessageDigest digest) {
    digest.update(value().duplicate());
    ...
}
```

```java
// AFTER — counter cells use normalized context digest
public void digest(MessageDigest digest) {
    if (isCounterCell()) {
        CounterContext.instance().updateDigest(digest, value());
    } else {
        digest.update(value().duplicate());
    }
    ...
}
```

---

## How to Spot

When a data type has a non-canonical wire encoding (like distributed counter contexts with per-replica shards), any code path that hashes or compares raw bytes must use the type-aware normalizer. Search for direct `digest.update(value())` calls on cells and verify that counter cells are handled separately.

---

## Test

The existing `CounterCellTest.testDigest` test was updated to call `cell.digest()` through the public API rather than directly invoking `CounterContext.instance().updateDigest()`, verifying that the general digest path now correctly normalizes counter values.
