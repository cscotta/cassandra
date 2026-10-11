# Bug 0911: Purgeable Tombstones Read-Repaired to Live Replicas

**Commit**: d3db33c008 · **Ticket**: CASSANDRA-11427
**Subsystem**: Read repair / ColumnFamilyStore range read
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
During a range slice read, Cassandra performs read repair by comparing results across replicas and sending missing data back. The result set for a row was assembled and handed to the read-repair mechanism before tombstones that were already past their `gc_grace_seconds` window (purgeable) were stripped. This caused purgeable tombstones to be unnecessarily shipped to replicas that did not have them, even though those tombstones would be compacted away anyway. Worse, once injected onto a replica that had already compacted them away, the re-introduced tombstone could shadow live data written between the original tombstone and the purge window, resulting in data appearing deleted when it should be visible. The fix calls `data.purgeTombstones(gcBefore(filter.timestamp))` on each partition's data immediately before it is added to the result set, ensuring purgeable tombstones are never included in read-repair mutations.

### Before / After
```java
// BEFORE — tombstones included in read-repair result set
removeDroppedColumns(data);
rows.add(new Row(rawRow.key, data));

// AFTER — purgeable tombstones stripped before adding to result
removeDroppedColumns(data);
data.purgeTombstones(gcBefore(filter.timestamp));  // strip purgeable tombstones
rows.add(new Row(rawRow.key, data));
```

---

## How to Spot
Read-repair paths must mirror the compaction tombstone-purging logic: if a compaction would discard a tombstone before shipping it to another node, read repair should do the same. Any place where partition data is assembled for cross-replica comparison should be checked for a `purgeTombstones` or equivalent call. The absence of such a call on the range-read path (while the single-partition path handled it) is the tell-tale inconsistency to look for.

---

## Test
No test added (CHANGES.txt updated only).
