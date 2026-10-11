# Bug 0218: Data Corruption Handling Issues in Cell and DeletionTime Deserialization

**Commit**: a565711056 · **Ticket**: CASSANDRA-18676
**Subsystem**: db/rows (Cell, UnfilteredSerializer, DeletionTime, SSTableIdentityIterator)
**Category**: Logic error in condition

---

## Root Cause

Three related defects allowed corrupted SSTable data to silently produce invalid state instead of being detected and routed through corruption-handling paths:

1. `DeletionTime.validate()` unconditionally returned `true`, so a negative `markedForDeleteAt` on a non-LIVE deletion was never flagged as invalid.
2. `Cell.deserialize()` and `UnfilteredSerializer` both handled a negative `localDeletionTime` by translating it to `INVALID_DELETION_TIME`, but they did not catch the case where `localDeletionTime < ttl` — an impossible combination that would cause a negative expiry timestamp on compaction. Negative timestamps and TTLs were also not rejected during deserialization.
3. `SSTableIdentityIterator` only caught `IndexOutOfBoundsException` and `VIntOutOfRangeException` as evidence of corruption; an `AssertionError` thrown by the newly added assertions (e.g., negative timestamp) was not caught, causing an unchecked exception rather than a `CorruptSSTableException` that would mark the file suspect.

### Before / After

```java
// BEFORE – DeletionTime.validate() always valid
public boolean validate() {
    return true;
}

// BEFORE – Cell.deserialize: no check for negative timestamp/TTL; localDeletionTime < ttl not caught
if (localDeletionTime < 0)
    localDeletionTime = helper.version < MessagingService.VERSION_50
                        ? INVALID_DELETION_TIME
                        : deletionTimeUnsignedIntegerToLong((int) localDeletionTime);
```

```java
// AFTER – DeletionTime.validate() checks markedForDeleteAt
public boolean validate() {
    return markedForDeleteAt >= 0 || isLive();
}

// AFTER – Cell.deserialize: reject negative timestamp/TTL, use decodeLocalDeletionTime
if (timestamp < 0) throw new IOException("Invalid negative timestamp: " + timestamp);
if (ttl < 0)       throw new IOException("Invalid TTL: " + ttl);
localDeletionTime = decodeLocalDeletionTime(localDeletionTime, ttl, helper);
// decodeLocalDeletionTime also handles localDeletionTime < ttl as INVALID_DELETION_TIME
```

---

## How to Spot

Corruption-detection logic should treat *any* invariant violation that cannot occur in a healthy file as an immediate signal, not just overflow conditions. When reviewing deserialization code, verify that: (a) all newly added assertions or precondition checks are guarded by corresponding `try/catch` blocks in the iteration layer so that `AssertionError` reaches `markSuspect`; and (b) combinations of fields that are logically impossible (e.g., `localDeletionTime < ttl` for a non-expired liveness) are explicitly handled rather than left to produce silently wrong results downstream.

---

## Test

`ReadCommandTest` was updated to use non-zero timestamps (> 0) to avoid triggering the new negative-timestamp assertion; `CorruptedSSTablesCompactionsTest` was refined with a clarifying comment about the retry loop.
