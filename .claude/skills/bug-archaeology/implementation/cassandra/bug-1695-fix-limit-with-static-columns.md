# Bug 1695: Fix LIMIT with Static Columns

**Commit**: abc6565e26 · **Ticket**: CASSANDRA-6956
**Subsystem**: src/java/org/apache/cassandra/db/filter/ColumnCounter.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ColumnCounter.GroupByPrefix` counted CQL rows by detecting group boundaries in a composite column name stream. Before this fix, it tracked only the last-seen component array (`last`) without distinguishing whether the first group was a static group. In tables with static columns the first "row" seen is the static group, which should count as one CQL row only if it has no corresponding regular rows in the partition. Without tracking whether the current group is static, the counter incremented for both the static group and the first regular group, causing `LIMIT N` to return N-1 actual data rows. The fix adds `previousGroupIsStatic` tracking: when the first column is static and a second (regular) group arrives, the static group's prior count is cancelled out by returning early.

### Before / After

```java
// BEFORE – no distinction between static and regular groups
private ByteBuffer[] last;

// in count():
if (last != null) {
    boolean isSameGroup = true;
    for (int i = 0; i < toGroup; i++) {
        if (ByteBufferUtil.compareUnsigned(last[i], current[i]) != 0) {
            isSameGroup = false; break;
        }
    }
    if (isSameGroup) return;
}
live++;
last = current;
```

```java
// AFTER – static group detected and compensated
private ByteBuffer[] previous;
private boolean previousGroupIsStatic;

// in count():
if (previous == null) {
    previousGroupIsStatic = type.isStaticName(column.name());
} else {
    // ... compare groups ...
    if (previousGroupIsStatic) {
        previous = current;
        previousGroupIsStatic = false;
        return;  // cancel the static row count
    }
}
live++;
previous = current;
```

---

## How to Spot

When counting logical rows across a mixed static/regular column stream, verify that the counter distinguishes static-only rows from regular rows. A counter that treats both identically will over-count when a static group precedes regular groups in the same partition.

---

## Test

No dedicated test added in this commit; a follow-up fix (commit d8c29a3f7e) corrected a secondary bug in this same logic.
