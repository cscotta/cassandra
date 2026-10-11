# Bug 0986: Materialized View Liveness Uses Wrong Comparator Direction

**Commit**: caedc8fdcf · **Ticket**: CASSANDRA-10910
**Subsystem**: Materialized Views
**Category**: Logic error in condition

---

## Root Cause
A bad merge replaced a generic `valueIfSet` helper with a hard-coded `minValueIfSet` helper and applied it to all three liveness fields (TTL, timestamp, localDeletionTime). The correct semantics differ per field: TTL should take the minimum (the row expires at the earliest TTL), timestamp should take the maximum (the latest write wins), and localDeletionTime should also take the maximum (a later deletion covers earlier writes). Using `min` for timestamp and localDeletionTime caused the view to record stale timestamps and miss deletions.

### Before / After
```java
// BEFORE — same min() applied to all three fields
this.viewClusteringLocalDeletionTime = minValueIfSet(viewClusteringLocalDeletionTime, ...);
this.viewClusteringTimestamp         = minValueIfSet(viewClusteringTimestamp, ...);
this.viewClusteringTtl               = minValueIfSet(viewClusteringTtl, ...);

// AFTER — per-field comparator
final Comparator max = Comparator.naturalOrder();
final Comparator min = Comparator.reverseOrder();
this.viewClusteringTtl               = valueIfSet(viewClusteringTtl, ttl, NO_TTL, min);
this.viewClusteringTimestamp         = valueIfSet(viewClusteringTimestamp, timestamp, NO_TIMESTAMP, max);
this.viewClusteringLocalDeletionTime = valueIfSet(viewClusteringLocalDeletionTime, localDeletionTime, NO_DELETION_TIME, max);
```

---

## How to Spot
Whenever multiple temporal metadata fields (TTL, timestamp, deletion time) are combined using the same aggregation function, verify that each field's semantics match the chosen operator. TTL represents expiry duration (want minimum), timestamp represents write time (want maximum), and deletion time represents coverage (want maximum).

---

## Test
`ViewTest.testRegularColumnTimestampUpdates` added — verifies that a sequence of column updates and timestamp-explicit updates produces the correct final view row with the highest timestamp and latest value.
