# Bug 2792: Hint Deletion Blocks Gossip Thread

**Commit**: 59f87afa7a · **Ticket**: CASSANDRA-2073

## Detection Rule
When a long-running I/O operation (flush + major compaction) is called synchronously from a gossip event handler, it blocks the gossip thread, preventing heartbeat updates and potentially causing false failure detection.

## Code Smell
`deleteHintsForEndPoint` calls `rm.apply()`, `hintStore.forceFlush()`, and `CompactionManager.instance.submitMajor(...).get()` directly on the caller's thread. If the caller is a gossip-triggered callback (e.g., `onDead`), the gossip thread stalls for the duration of the compaction.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: sync-io-on-gossip-thread
- severity: hang
- subsystem: gossip

## Root Cause
`HintedHandOffManager.deleteHintsForEndPoint` was a synchronous method that applied a deletion mutation, flushed the hints column family store, and then waited for a blocking major compaction to complete (`submitMajor(...).get()`). Gossip event handlers call this when a node is detected as dead. Blocking the gossip thread for seconds-to-minutes of compaction prevents heartbeat processing, causing other nodes to falsely report more failures. The fix wraps the entire operation in a `Runnable` submitted to `StorageService.scheduledTasks`, returning immediately from the gossip callback while the I/O proceeds asynchronously. The blocking `.get()` on the compaction future is also removed in favor of fire-and-forget.

## Example
```java
// BEFORE: synchronous, blocks caller (gossip thread)
public static void deleteHintsForEndPoint(InetAddress endpoint) {
    ColumnFamilyStore hintStore = ...;
    RowMutation rm = ...; rm.delete(...);
    rm.apply();
    hintStore.forceFlush();
    CompactionManager.instance.submitMajor(hintStore, 0, Integer.MAX_VALUE).get(); // BLOCKS
}
```
```java
// AFTER: asynchronous, gossip thread returns immediately
public static void deleteHintsForEndPoint(final InetAddress endpoint) {
    final RowMutation rm = ...; rm.delete(...);
    Runnable runnable = new Runnable() {
        public void run() {
            rm.apply();
            hintStore.forceFlush();
            CompactionManager.instance.submitMajor(hintStore, 0, Integer.MAX_VALUE); // no .get()
        }
    };
    StorageService.scheduledTasks.execute(runnable);
}
```
