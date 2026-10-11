# Bug 0305: FileStreamTask Interruption Corrupts Shared Channel Proxy for System Table

**Commit**: a00d8fd5ba · **Ticket**: CASSANDRA-17663
**Subsystem**: src/java/org/apache/cassandra/streaming/async/NettyStreamingMessageSender.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`FileStreamTask` acquired the preferred IP for the remote endpoint by calling `template.connectTo()` lazily inside the executor thread, but that thread could be interrupted mid-execution (e.g., while a concurrent repair abort was in progress). When interrupted, the `Thread.interrupted()` flag was set, and any subsequent blocking call on the shared `ChannelProxy` used by the system-table streaming pipeline would throw an `InterruptedException`, silently corrupting the shared connection state.

In `LocalSyncTask`, `abort()` held the intrinsic lock and checked a plain `boolean active` field, creating a race between `startSync()` setting `streamPlan` and `abort()` reading it: if `abort()` ran before `streamPlan` was assigned, it reset `active = false` without actually aborting the in-flight plan, and the plan's cleanup was then skipped entirely.

### Before / After

```java
// BEFORE – plain boolean, synchronized method, streamPlan set after execution starts
private boolean active = true;
private StreamPlan streamPlan;

protected synchronized void startSync() {
    if (active) {
        streamPlan = createStreamPlan();
        streamPlan.execute();
    }
}

public synchronized void abort() {
    if (active) {
        if (streamPlan == null) {
            active = false;   // races with startSync
            set(stat);
        } else {
            streamPlan.getCoordinator().getAllStreamSessions().forEach(StreamSession::abort);
        }
    }
}
```

```java
// AFTER – CompletableFuture hands the plan to abort() without any lock
private final AtomicBoolean active = new AtomicBoolean(true);
private final CompletableFuture<StreamPlan> planFuture = new CompletableFuture<>();

protected void startSync() {
    if (active.get()) {
        StreamPlan plan = createStreamPlan();
        plan.execute();
        planFuture.complete(plan);
    }
}

public void abort() {
    planFuture.whenComplete((plan, cause) ->
        plan.getCoordinator().getAllStreamSessions().forEach(StreamSession::abort));
}
```

---

## How to Spot

When a task running in an executor thread can be interrupted, any use of a captured mutable object whose methods are not interrupt-safe should be pre-computed on the caller thread and passed as a parameter. Review all executor-submitted `Runnable`/`Callable` objects for lazy resource acquisition (network connections, preferred IPs, shared proxies) that occurs inside the thread: if the acquisition involves blocking I/O or JMX lookups, an interrupt can leave the shared resource in a broken state for other callers.

---

## Test

`RepairErrorsTest.testRemoteStreamFailure` uses ByteBuddy to inject an `IOException` into the stream reader on node 3 and an interruptible sleep into `getPreferredIP` on node 1, then asserts that the peers table remains compactable and no disk-failure policy is triggered after the interrupted repair.
