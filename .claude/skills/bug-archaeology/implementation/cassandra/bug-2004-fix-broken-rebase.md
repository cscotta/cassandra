# Bug 2004: Read Repair Metrics Lost in Bad Rebase

**Commit**: 4c0be33ea0 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/metrics/ReadRepairMetrics.java, src/java/org/apache/cassandra/service/AbstractReadExecutor.java, src/java/org/apache/cassandra/service/ReadCallback.java, src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: State not cleaned up

---

## Root Cause

A bad rebase dropped the entire `ReadRepairMetrics` class and its integration points in `AbstractReadExecutor`, `ReadCallback`, and `StorageProxy`. As a result, read repair activity (attempted, blocking-repaired, background-repaired) was no longer tracked or exposed via JMX or `nodetool tpstats`. The fix re-introduces the `ReadRepairMetrics` class and re-wires all three call sites where the meters should be marked, along with the JMX accessors and nodetool output.

### Before / After

```java
// BEFORE: no metrics tracked, ReadRepairMetrics class absent
// AbstractReadExecutor.getReadExecutor():
List<InetAddress> queryTargets = consistency_level.filterForQuery(table, allReplicas, metaData.newReadRepairDecision());
```

```java
// AFTER: read repair decision tracked before filtering
ReadRepairDecision rrDecision = metaData.newReadRepairDecision();
if (rrDecision != ReadRepairDecision.NONE)
    ReadRepairMetrics.attempted.mark();
List<InetAddress> queryTargets = consistency_level.filterForQuery(table, allReplicas, rrDecision);
```

---

## How to Spot

After a rebase, verify that new classes referenced in compile-time imports actually exist in the tree. A rebase that silently drops a new file while retaining the code that uses it will compile only if the file was not yet referenced — check for newly introduced import statements pointing to classes that do not exist.

---

## Test

No test added.
