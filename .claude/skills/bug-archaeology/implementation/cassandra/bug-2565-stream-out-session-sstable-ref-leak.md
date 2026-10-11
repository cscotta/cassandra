# Bug 2565: StreamOutSession holds SSTable references forever when remote endpoint dies

**Commit**: 6a8b904dd8 · **Ticket**: CASSANDRA-3216

## Detection Rule
When a streaming session object holds reference-counted SSTable handles but has no mechanism to detect that the remote peer has gone down, the references are never released and the SSTables cannot be deleted or compacted.

## Code Smell
A streaming session class does not implement `IFailureDetectionEventListener` or `IEndpointStateChangeSubscriber`; its `close()` method is only reachable via normal completion, leaving no path to release SSTable references on peer failure.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: missing-failure-listener
- severity: data-loss
- subsystem: streaming

## Root Cause
`StreamOutSession` held `PendingFile` objects which in turn held `SSTableReader` references. If the destination node died during streaming, nothing called `close()` on the session, so `releaseReference()` was never invoked for any of the pending files. The SSTables would remain pinned in memory and on disk, potentially forever. The fix registers the session with `Gossiper` and `FailureDetector` on construction and overrides `convict()`/`onRemove()`/`onRestart()` to call `close(false)`, ensuring references are always released.

## Example
```java
// BEFORE: close() only called on success; no failure listener registered
public void close() {
    for (PendingFile file : files.values())
        file.sstable.releaseReference();
    streams.remove(context);
    if (callback != null) callback.run();
}
```
```java
// AFTER: registers with failure detector; convict() triggers close
public void convict(InetAddress endpoint, double phi) {
    if (!endpoint.equals(getHost())) return;
    if (phi < 2 * DatabaseDescriptor.getPhiConvictThreshold()) return;
    close(false); // releases all SSTable references
}
```
