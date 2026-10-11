# Bug 1873: Fix Potential NPE While Loading Paxos State

**Commit**: 1240c9bd22 · **Ticket**: CASSANDRA-6211
**Subsystem**: src/java/org/apache/cassandra/db/SystemKeyspace.java
**Category**: Missing null / bounds check

---

## Root Cause

When loading Paxos state from the system keyspace, the code unconditionally called `row.getUUID("in_progress_ballot")` to construct the `promised` `Commit`. If a row existed but had no `in_progress_ballot` column (e.g. after a crash before a ballot was written, or partial compaction), `getUUID` returned `null`, causing a `NullPointerException` when the UUID was subsequently used. The fix mirrors the pattern already used for the `proposal` column: check `row.has(...)` before accessing the value.

### Before / After

```java
// BEFORE – unconditional access; NPE if column is absent
Commit promised = new Commit(key, row.getUUID("in_progress_ballot"),
        EmptyColumns.factory.create(metadata));
```

```java
// AFTER – guard with has() check, fall back to empty commit
Commit promised = row.has("in_progress_ballot")
        ? new Commit(key, row.getUUID("in_progress_ballot"),
                EmptyColumns.factory.create(metadata))
        : Commit.emptyCommit(key, metadata);
```

---

## How to Spot

Whenever reading optional or sparse columns from a Cassandra result row, always use `row.has(columnName)` before calling typed getters. Absent columns return `null` from typed getters, which will NPE on unboxing or UUID construction.

---

## Test

No test added.
