# Bug 1671: Fix Batchlog to Account for CF Truncation Records

**Commit**: 87097066e7 · **Ticket**: CASSANDRA-6999
**Subsystem**: src/java/org/apache/cassandra/db/ (BatchlogManager, SystemTable)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When replaying a batchlog entry, `BatchlogManager` would re-apply every mutation stored in the batch, even if the column family it targeted had been truncated after the batch was written. This meant that data explicitly erased by a `TRUNCATE` command could be resurrected by a concurrent or subsequent batchlog replay. The fix checks each mutation's column families against the truncation timestamp stored in `SystemTable`; any CF truncated at or after the batch's write timestamp is stripped from the mutation before replay, and the batch is skipped entirely if nothing remains.

### Before / After

```java
// BEFORE – replays all mutations without checking truncation
for (int i = 0; i < size; i++)
    replaySerializedMutation(RowMutation.serializer.deserialize(in, VERSION), writtenAt, rateLimiter);
```

```java
// AFTER – strips truncated CFs before replaying
for (UUID cfId : mutation.getColumnFamilyIds())
    if (writtenAt <= SystemTable.getTruncatedAt(cfId))
        mutation = mutation.without(cfId);
if (!mutation.isEmpty())
    mutations.add(mutation);
```

---

## How to Spot

Any replay or retry subsystem (batchlog, hinted handoff, commit log replay) must consult truncation records before reapplying mutations. If the replay path only checks whether data is "too old" via TTL but ignores explicit truncation events, `TRUNCATE` semantics will be violated.

---

## Test

`BatchlogManagerTest` was expanded with scenarios that write a batch, truncate the target table, and then assert that the batchlog replay does not restore truncated rows.
