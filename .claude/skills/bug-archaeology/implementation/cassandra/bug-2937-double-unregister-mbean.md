# Bug 2937: Double unregisterMBean on concurrent shutdown calls crashes JMXEnabledThreadPoolExecutor

**Commit**: 433a0f9089 · **Ticket**: n/a

## Detection Rule
When `shutdown()` and `shutdownNow()` can be called concurrently and both unconditionally call `unregisterMBean()`, the second call will throw a `InstanceNotFoundException`, which was previously swallowed but now re-thrown.

## Code Smell
`unregisterMBean()` called unconditionally in both `shutdown()` and `shutdownNow()` without checking `isShutdown()` first; the MBean is already unregistered on the second call.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: unguarded-idempotent-deregistration
- severity: crash
- subsystem: network-messaging

## Root Cause
`JMXEnabledThreadPoolExecutor.shutdown()` and `shutdownNow()` both called `unregisterMBean()` without first checking if the executor was already shut down. If either method was invoked twice, or both were called concurrently, the MBean would be unregistered twice, throwing an exception. The fix wraps `unregisterMBean()` in an `if (!isShutdown())` guard inside a `synchronized` block (since `isShutdown()` itself is not atomically paired with the unregister operation).

## Example
```java
// BEFORE
public void shutdown() {
    unregisterMBean();  // unconditional — throws if called twice
    super.shutdown();
}
```
```java
// AFTER
public synchronized void shutdown() {
    if (!isShutdown()) {
        unregisterMBean();
    }
    super.shutdown();
}
```
