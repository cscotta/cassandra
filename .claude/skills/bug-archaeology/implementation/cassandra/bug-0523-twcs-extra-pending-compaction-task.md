# Bug 0523: TWCS Reports Extra Pending Compaction Task in nodetool compactionstats

**Commit**: 122cf57f11 · **Ticket**: CASSANDRA-15409
**Subsystem**: src/java/org/apache/cassandra/db/compaction/TimeWindowCompactionStrategy.java
**Category**: Logic error in condition

---

## Root Cause

`TimeWindowCompactionStrategy.getNextBackgroundTask()` called `updateEstimatedCompactionsByTasks()` before calling `newestBucket()`. Because `newestBucket()` returned early as soon as it found the first eligible bucket, `updateEstimatedCompactionsByTasks()` counted all eligible windows (including ones that `newestBucket()` would skip), inflating `estimatedRemainingTasks` by including windows beyond the one that would actually be compacted next. This made `nodetool compactionstats` show more pending tasks than were actually queued.

The fix embeds estimated-task counting inside `newestBucket()` itself, returning a `NewestBucket` value object that carries both the selected SSTable list and the computed estimate. Because the counting now happens in a single pass that accounts for all eligible buckets (not just the first), the estimate is accurate and consistent with what the strategy will actually schedule.

### Before / After

```java
// BEFORE — estimation computed separately before bucket selection; may double-count
updateEstimatedCompactionsByTasks(buckets.left);
List<SSTableReader> mostInteresting = newestBucket(...);
if (!mostInteresting.isEmpty()) return mostInteresting;
```

```java
// AFTER — estimation embedded in newestBucket, returned together
NewestBucket mostInteresting = newestBucket(...);
this.estimatedRemainingTasks = mostInteresting.estimatedRemainingTasks;
if (!mostInteresting.sstables.isEmpty()) return mostInteresting.sstables;
```

---

## How to Spot

When a compaction strategy computes `estimatedRemainingTasks` in a separate method from the one that selects actual work, the two computations can diverge in what they count. Any time an estimation loop and a selection loop both iterate the same bucket set but with different early-exit semantics, check whether they are truly counting the same things.

---

## Test

`TimeWindowCompactionStrategyTest` is extended to assert that `estimatedRemainingTasks` is zero when the bucket is below the minimum threshold, one when a single bucket is eligible, and `buckets.keySet().size() + 1` when multiple eligible buckets exist with overflow.
