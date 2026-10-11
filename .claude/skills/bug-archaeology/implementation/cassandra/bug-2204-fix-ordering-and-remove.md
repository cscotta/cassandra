# Bug 2204: Tracing Event ID Generated Inside Async Task Causes Non-Deterministic Ordering

**Commit**: 1ded4ff294 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/tracing/TracingAppender.java, src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Race condition / missing synchronization

---

## Root Cause

The tracing `event_id` (a Type 1 UUID used as the clustering column in `system_traces.events`) was generated *inside* the async TRACING stage `Runnable`, meaning its timestamp was based on when the task was dequeued and executed rather than when the log event actually occurred. Because multiple trace events could be queued and executed out of order, the UUID-based ordering of rows in the events table did not match the actual sequence of traced operations. Moving the UUID generation to before the `Runnable` is submitted captures the correct wall-clock timestamp relative to the event. The `happened_at` timestamp column was also removed as redundant given the corrected event_id ordering.

### Before / After

```java
// BEFORE: eventId generated at dequeue time, inside the async task
StageManager.getStage(Stage.TRACING).execute(new WrappedRunnable() {
    public void runMayThrow() throws Exception {
        ByteBuffer eventId = ByteBufferUtil.bytes(
            UUIDGen.makeType1UUIDFromHost(FBUtilities.getBroadcastAddress()));
        ...
        addColumn(cf, buildName(cfMeta, eventId, bytes("happened_at")), event.getTimeStamp());
```

```java
// AFTER: eventId captured at submission time, before the async task
final ByteBuffer eventId = ByteBufferUtil.bytes(
    UUIDGen.makeType1UUIDFromHost(FBUtilities.getBroadcastAddress()));
StageManager.getStage(Stage.TRACING).execute(new WrappedRunnable() {
    public void runMayThrow() throws Exception {
        // eventId already set; happened_at column removed
```

---

## How to Spot

When an identifier or timestamp is used as a sort key and is generated inside an async task, check whether it must reflect the time of the triggering event rather than the time of execution. Capture time-sensitive values before handing work off to a thread pool.

---

## Test

No test added.
