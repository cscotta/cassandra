# Bug 0678: nodetool tablestats Reports "Number of keys" Instead of "Number of partitions"

**Commit**: 16a5e8217f · **Ticket**: CASSANDRA-13722
**Subsystem**: `src/java/org/apache/cassandra/tools/nodetool/TableStats.java`
**Category**: Wrong constant / default value

---

## Root Cause

The `tablestats` output line used the label "Number of keys (estimate)" for the partition count metric. In Cassandra 3.x the term "key" is ambiguous — it can refer to a primary key, partition key, or clustering key — whereas the metric tracks partition counts. The misleading label was a copy from an older version of the tool and was never updated when the storage model changed terminology.

### Before / After

```java
// BEFORE
System.out.println("\t\tNumber of keys (estimate): " + estimatedPartitionCount);
```

```java
// AFTER
System.out.println("\t\tNumber of partitions (estimate): " + estimatedPartitionCount);
```

---

## How to Spot

Output labels in diagnostic tools should match the current domain vocabulary. Any output string containing "key" in a context where "partition" is the modern term is a candidate for a terminology audit. This is easy to overlook during refactors that rename storage concepts but only update code, not user-facing strings.

---

## Test

No test added.
