# Bug 0415: Group By Pager Incorrectly Triggers Short-Read Protection, Returning Wrong or Empty Results

**Commit**: f258ae6751 · **Ticket**: CASSANDRA-16307
**Subsystem**: src/java/org/apache/cassandra/db/filter/DataLimits.java, src/java/org/apache/cassandra/service/DataResolver.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Two bugs interacted. First, the `GroupByAwareCounter` used a boolean field named `hasGroupStarted` whose intended semantics were "there is an in-progress group with at least one live row that has not yet been closed"; the name implied "a group has ever been started" and was used in both directions, causing incorrect group counting at partition boundaries, particularly when all rows in a group were deleted. Second, in `DataResolver`, the condition used to skip Short-Read Protection (SRP) reads was `!singleResultCounter.isDone()` (skip SRP when the counter is *not* done), which is the opposite of the intended guard: SRP should be skipped when the result set is *already exhausted*. The correct check was `command.limits().isExhausted(singleResultCounter)`.

### Before / After

```java
// BEFORE (DataResolver) — skips SRP when counter is not done (wrong direction)
if (!singleResultCounter.isDone() && command.limits().perPartitionCount() == DataLimits.NO_LIMIT)
    return null;
```

```java
// AFTER — skips SRP when limits are actually exhausted
if (command.limits().isExhausted(singleResultCounter) && command.limits().perPartitionCount() == DataLimits.NO_LIMIT)
    return null;
```

```java
// BEFORE (DataLimits) — ambiguous field name leading to incorrect group state tracking
protected boolean hasGroupStarted;
```

```java
// AFTER — renamed for clarity; semantics unchanged but correctly reasoned about
protected boolean hasUnfinishedGroup;
```

---

## How to Spot

When a boolean flag is used both as "a thing has started" and "a thing is still open", the two readings diverge whenever the thing can be closed without starting a new one. Rename such flags to their precise invariant ("unfinished", "pending", "open") so that every branch that sets or clears the flag can be reviewed against the single correct meaning.

---

## Test

`GroupByTest.groupByWithDeletesAndSrpOnPartitions` verifies that GROUP BY queries with deleted rows return the correct (empty) result set across multiple paging sizes and LIMIT variants on a 2-node cluster.
