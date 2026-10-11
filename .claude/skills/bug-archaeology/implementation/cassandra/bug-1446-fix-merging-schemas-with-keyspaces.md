# Bug 1446: Fix Merging Schemas With Re-Dropped Keyspaces

**Commit**: 0393c30633 · **Ticket**: CASSANDRA-7256
**Subsystem**: src/java/org/apache/cassandra/db/DefsTables.java
**Category**: Logic error in condition

---

## Root Cause

`DefsTables.mergeKeyspaces()` compared "before" and "after" schema snapshots using `Maps.difference()`, but the logic for classifying changed entries was flawed. A keyspace that had been created and then dropped on a remote node arrives as an entry in `entriesDiffering()` where the post-value has zero live columns. The old code treated any entry in `entriesDiffering()` with a non-empty pre-value and empty post-value as a drop — but it also tried to process re-creations mixed inside the same loop, failing to account for the case where both pre and post are empty (keyspace created-and-dropped before ever reaching this node) or where only the post has live columns (a re-creation after a prior drop). This could trigger incorrect `addKeyspace`, `updateKeyspace`, or `dropColumnFamily` calls, corrupting the local schema view.

### Before / After

```java
// BEFORE — re-created keyspaces checked in separate pass with fragile zero-column test
if (prevValue.getColumnCount() == 0)
{
    addKeyspace(...);   // re-creation path
    continue;
}
leftToProcess.add(entry.getKey());
// ... then later:
if (newState.getColumnCount() == 0)
    keyspacesToDrop.add(...);
else
    updateKeyspace(...);  // missed the recreate-after-drop case
```

```java
// AFTER — explicit four-way classification
if (pre.getColumnCount() > 0 && post.getColumnCount() > 0)
    altered.add(keyspaceName);
else if (pre.getColumnCount() > 0)
    dropped.add(keyspaceName);
else if (post.getColumnCount() > 0) // a (re)created keyspace
    created.add(new Row(entry.getKey(), post));
// else: both empty — keyspace created+dropped before reaching us; ignore
```

---

## How to Spot

Schema merge logic that classifies entries using a single `getColumnCount() == 0` test on only one side of a difference is missing cases. Any diff-and-classify algorithm over distributed state snapshots must explicitly enumerate all four quadrants: (pre empty, post empty), (pre non-empty, post empty), (pre empty, post non-empty), and (both non-empty). Failing to handle all quadrants leaves at least one scenario silently unhandled or misclassified.

---

## Test

No test added.
