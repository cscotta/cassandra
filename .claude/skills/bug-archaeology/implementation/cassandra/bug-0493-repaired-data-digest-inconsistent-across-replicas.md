# Bug 0493: Repaired Data Digest Inconsistent Across Replicas Due to Differing Read Limits

**Commit**: a8e7cfbc0e · **Ticket**: CASSANDRA-15601
**Subsystem**: src/java/org/apache/cassandra/db/ReadCommand.java, src/java/org/apache/cassandra/db/RepairedDataInfo.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The repaired-data digest, used to detect inconsistencies between replicas during reads, was computed over however many rows each replica happened to read before the query limit was satisfied. Because replicas may have different amounts of repaired vs. unrepaired data, one replica could stop reading repaired sstables partway through a partition while another replica read further — producing different digests even when the repaired data was identical. The fix introduces a dedicated counter that tracks repaired-data reads independently of the query limit, and extends the repaired-data iterator to silently "over-read" up to the query limit on repaired sstables after the result set is satisfied, ensuring all replicas compute their digest over the same logical amount of repaired data.

### Before / After

```java
// BEFORE: single counter, stops at query limit
iterator = limits().filter(iterator, nowInSec(), selectsFullPartition());
// repairedDataInfo reads only as far as the limit iterator allows
```

```java
// AFTER: separate counter for repaired data, over-reads silently
DataLimits.Counter limit = limits().newCounter(...);
iterator = limit.applyTo(iterator);
// extend reads repaired data up to limits() beyond what was returned
iterator = repairedDataInfo.extend(iterator, limit);
```

---

## How to Spot

Any feature that computes a digest or fingerprint over a subset of data that can be bounded by an external query limit is at risk of this class of bug: the bounding condition may differ per replica, causing divergent digests on identical data. Look for digest computation interleaved with row-count or byte-count limiting iterators where the limit is applied globally rather than separately per digest computation.

---

## Test

`RepairDigestTrackingTest` (distributed test) verifies that repaired data digests are consistent across replicas; `ReadCommandTest` and `RepairedDataInfoTest` add unit coverage for the new counter-based over-read mechanism.
