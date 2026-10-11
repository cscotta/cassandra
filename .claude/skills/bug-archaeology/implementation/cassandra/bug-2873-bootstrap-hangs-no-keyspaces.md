# Bug 2873: Bootstrap hangs indefinitely when no keyspaces are defined

**Commit**: 477a59eec0 · **Ticket**: CASSANDRA-1574

## Detection Rule
When a blocking wait loop polls a boolean flag that is only set inside a callback, verify the callback will actually be invoked even when the work queue is empty (zero items to bootstrap).

## Code Smell
`StorageService` spun in `while (isBootstrapMode) { Thread.sleep(100); }` waiting for `finishBootstrapping()` to clear the flag, but `finishBootstrapping` was called only from `BootStrapper` callbacks; if no keyspaces existed, no callbacks fired and the loop never terminated.

## Tags
- scope: cassandra
- category: hang
- code-pattern: spin-wait-on-callback-that-never-fires
- severity: hang
- subsystem: bootstrap-startup

## Root Cause
`BootStrapper.startBootstrap` built a completion callback per source node and called `finishBootstrapping` when all callbacks fired. With zero keyspaces, zero callbacks were registered and the `CountDownLatch` was never created, so the bootstrap flag was never cleared. Replacing the polling loop with `latch.await()` followed by a direct call to `finishBootstrapping()` after the loop makes bootstrap always complete, even when the latch starts at zero.

## Example
```java
// BEFORE — spins waiting for a callback that never fires when no tables exist
startBootstrap(token);
while (isBootstrapMode) {
    Thread.sleep(100); // hangs forever if no keyspaces
}
```
```java
// AFTER — bootstrap() blocks on latch then calls finish directly
latch.await();
StorageService.instance.finishBootstrapping();
```
