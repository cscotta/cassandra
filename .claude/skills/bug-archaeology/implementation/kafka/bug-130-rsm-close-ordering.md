**Commit**: f0c3d93104 · **Ticket**: KAFKA-19597
**Subsystem**: server/src/main/java/org/apache/kafka/server/log/remote/storage/RemoteLogManager.java
**Category**: Resource leak

**Root cause**
During broker shutdown `RemoteLogManager.close()` closed the `RemoteStorageManager` and `RemoteLogMetadataManager` plugins before closing the remote-log reader thread pools. Any in-flight request being processed by the reader threads would therefore attempt to call into an already-closed RSM, producing errors instead of completing gracefully. Reordering the close sequence so that the reader thread pools are drained first ensures all in-progress requests complete before the storage backends are torn down.

**Before/After**
```java
// BEFORE – RSM closed while reader threads may still be running
Utils.closeQuietly(remoteStorageManagerPlugin, "remoteStorageManagerPlugin");
Utils.closeQuietly(remoteLogMetadataManagerPlugin, "remoteLogMetadataManagerPlugin");
Utils.closeQuietly(indexCache, "RemoteIndexCache");

// AFTER – reader threads stopped first, then storage backends
Utils.closeQuietly(indexCache, "RemoteIndexCache");
Utils.closeQuietly(remoteLogMetadataManagerPlugin, "remoteLogMetadataManagerPlugin");
Utils.closeQuietly(remoteStorageManagerPlugin, "remoteStorageManagerPlugin");
```

**How to spot**
In any multi-component shutdown, components that depend on another must be closed before the dependency. If the dependency is closed first, concurrent users of that dependency (e.g., worker threads) will receive errors. Look for `close()` methods that close a service while thread-pool executors referencing that service are still running.

**Test**
`RemoteLogManagerTest` verifies the new close ordering with an `InOrder` mock assertion: `remoteLogMetadataManager.close()` is verified to precede `remoteStorageManager.close()`.
