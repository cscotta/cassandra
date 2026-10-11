# Bug 1297: Fix Merge from CASSANDRA-8359 — DTCS Uses All Uncompacting SSTables Instead of Strategy-Owned Subset

**Commit**: 0ba5f2721b · **Ticket**: CASSANDRA-8359
**Subsystem**: db/compaction/DateTieredCompactionStrategy.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

A merge of CASSANDRA-8359 introduced a regression in `DateTieredCompactionStrategy.getNextBackgroundTask()`. The code fetched `cfs.getUncompactingSSTables()` — the global set of all SSTables not currently being compacted — but DTCS should only consider the SSTables it owns (the `sstables` field). When there are multiple compaction strategies (e.g. during a schema migration), this made DTCS operate on SSTables belonging to other strategies, potentially triggering compactions on files it has no authority over and producing incorrect expiry calculations.

### Before / After

```java
// BEFORE
Set<SSTableReader> uncompacting = cfs.getUncompactingSSTables();
```

```java
// AFTER
Set<SSTableReader> uncompacting = Sets.intersection(sstables, cfs.getUncompactingSSTables());
```

---

## How to Spot

In any multi-strategy compaction architecture, a method that starts from the global CFS SSTable set rather than intersecting with the strategy's own `sstables` field is suspect. Reviewers should verify that expired-SSTable and candidate-selection logic is scoped to `this.sstables` before being further filtered by compaction state, not the other way around.

---

## Test

A test fix was added: `DateTieredCompactionStrategyTest` now calls `dtcs.addSSTable()` for each existing SSTable before calling `dtcs.startup()`, so the strategy's internal set is properly populated and the intersection produces a meaningful result.
