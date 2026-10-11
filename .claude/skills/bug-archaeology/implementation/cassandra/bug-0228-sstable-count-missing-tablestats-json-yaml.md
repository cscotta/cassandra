# Bug 0228: sstable_count Metric Missing from nodetool tablestats JSON/YAML Output

**Commit**: 08b9471a1e · **Ticket**: CASSANDRA-18448
**Subsystem**: tools/nodetool/stats
**Category**: Wrong constant / default value

---

## Root Cause

`TableStatsHolder` serializes per-table statistics into a `Map<String, Object>` for JSON/YAML rendering. The `sstable_count` field was never added to that map, so it was silently absent from machine-readable output even though it was present in the human-readable text output. A one-line `put` was missing.

### Before / After

```java
// BEFORE
mpTable.put("sstables_in_each_level", table.sstablesInEachLevel);
```

```java
// AFTER
mpTable.put("sstable_count", table.sstableCount);
mpTable.put("sstables_in_each_level", table.sstablesInEachLevel);
```

---

## How to Spot

When a struct has both a human-readable and a machine-readable serialization path, any new field addition must appear in both. Review serialization classes like `TableStatsHolder` to ensure that every field defined on the holder object is explicitly mapped; a diff between the holder's declared fields and the keys inserted into the map immediately exposes gaps.

---

## Test

No test added.
