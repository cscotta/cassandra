# Bug 2017: NPE in CommitLogReplayer When CF Has No Truncation Record

**Commit**: 57e6888e6a · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLogReplayer.java
**Category**: Missing null / bounds check

---

## Root Cause

During commit log replay, the code looked up a column family's truncation position from `truncationPositions` and immediately dereferenced `.left` on the result without checking for `null`. If the column family had never been truncated, the map returned `null` and the dereference threw a `NullPointerException`, preventing the node from replaying its commit log on startup.

### Before / After

```java
// BEFORE
ReplayPosition truncatedAt = truncationPositions.get(cfs.metadata.cfId).left;
```

```java
// AFTER
Pair<ReplayPosition, Long> truncateRecord = truncationPositions.get(cfs.metadata.cfId);
ReplayPosition truncatedAt = truncateRecord == null ? null : truncateRecord.left;
```

---

## How to Spot

Any map lookup whose result is immediately field-accessed without a null guard is a potential NPE. Pay special attention in startup/recovery paths where the data set may be sparse (e.g., column families that have never been truncated will have no entry).

---

## Test

No test added.
