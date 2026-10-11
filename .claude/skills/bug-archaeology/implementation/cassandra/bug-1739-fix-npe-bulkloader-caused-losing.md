# Bug 1739: Fix NPE on BulkLoader Caused by Losing StreamEvent

**Commit**: b3a9a44343 · **Ticket**: CASSANDRA-6636
**Subsystem**: src/java/org/apache/cassandra/streaming, src/java/org/apache/cassandra/tools
**Category**: Race condition / missing synchronization

---

## Root Cause

`BulkLoader.stream()` called `plan.execute()` to get a `StreamResultFuture` and then immediately called `future.addEventListener(this)` on the returned object. However, `execute()` internally starts the streaming sessions before returning, so stream events could be fired and consumed before the listener was registered, causing the `ProgressIndicator` listener to miss events. If a stream completed before the listener was attached, the future state was lost and a subsequent `future.planId` access could NPE.

The fix moves listener registration into `StreamPlan` before `execute()` is called, so listeners are in place before any session activity begins.

### Before / After

```java
// BEFORE (SSTableLoader.java)
StreamResultFuture bulkResult = plan.execute();
bulkResult.addEventListener(this);
return bulkResult;

// BEFORE (BulkLoader.java)
future = loader.stream(options.ignores);
// ... later ...
if (!options.noProgress)
    future.addEventListener(new ProgressIndicator());
```

```java
// AFTER (SSTableLoader.java)
plan.listeners(this, listeners);
return plan.execute();

// AFTER (BulkLoader.java)
if (options.noProgress)
    future = loader.stream(options.ignores);
else
    future = loader.stream(options.ignores, new ProgressIndicator());
```

---

## How to Spot

Any pattern of "start async operation, then register listener" is susceptible to missed events. When reviewing async/future-based code, check that all listeners are registered before the operation is initiated, not after the handle is returned.

---

## Test

No test added.
