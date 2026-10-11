# Bug 1259: RejectedExecutionException During Shutdown Logged as ERROR Instead of DEBUG

**Commit**: 9e9846e73d · **Ticket**: CASSANDRA-8564
**Subsystem**: src/java/org/apache/cassandra/concurrent/DebuggableScheduledThreadPoolExecutor.java
**Category**: Logic error in condition

---

## Root Cause

When Cassandra shuts down, scheduled thread pool executors are stopped before all submitted tasks have drained. Any task submitted or re-queued after the executor shuts down throws a `RejectedExecutionException`. Without a custom `RejectedExecutionHandler`, the JDK's default handler propagates that exception up through `afterExecute`, which `DebuggableScheduledThreadPoolExecutor` logs at ERROR level. These rejections are harmless during an orderly shutdown, but the absence of a shutdown-aware handler meant operators saw alarming ERROR noise in logs for expected behaviour.

### Before / After

```java
// BEFORE — no custom handler; RejectedExecutionException logged as ERROR on shutdown
public DebuggableScheduledThreadPoolExecutor(int corePoolSize, String threadPoolName, int priority)
{
    super(corePoolSize, new NamedThreadFactory(threadPoolName, priority));
    // default JDK AbortPolicy in effect
}
```

```java
// AFTER — custom handler suppresses expected rejections at shutdown to DEBUG
public static final RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler()
{
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
    {
        if (executor.isShutdown())
        {
            if (!StorageService.instance.isInShutdownHook())
                throw new RejectedExecutionException("ScheduledThreadPoolExecutor has shut down.");
            logger.debug("ScheduledThreadPoolExecutor has shut down as part of C* shutdown");
        }
        else
        {
            throw new AssertionError("Unknown rejection of ScheduledThreadPoolExecutor task");
        }
    }
};
```

---

## How to Spot

Any `ScheduledThreadPoolExecutor` subclass that does not install a custom `RejectedExecutionHandler` will use `AbortPolicy` by default, which throws unconditionally. In systems with a known shutdown sequence, reviewers should check whether executor shutdown order guarantees that no new tasks arrive after `shutdown()` — if not, a handler that distinguishes orderly shutdown from unexpected rejection is required to keep logs clean and meaningful.

---

## Test

No test added.
