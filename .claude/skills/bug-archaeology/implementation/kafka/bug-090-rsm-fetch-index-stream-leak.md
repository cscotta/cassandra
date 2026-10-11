**Commit**: be80e3cb8a · **Ticket**: KAFKA-18923
**Subsystem**: core/src/main/java/kafka/server/TierStateMachine.java
**Category**: Resource leak

**Root cause**: `readLeaderEpochCheckpoint()` obtained an `InputStream` from `RemoteStorageManager.fetchIndex(...)` and passed it to `new InputStreamReader(inputStream, ...)`. If the `BufferedReader` constructor or any subsequent read threw an exception, the raw `InputStream` was never closed because only the `BufferedReader` was covered by the `try-with-resources`. Similarly, `buildProducerSnapshotFile()` passed the result of `fetchIndex(...)` directly to `Files.copy(...)` without a `try-with-resources`, leaking the stream on any exception. Both were fixed by wrapping the raw stream in `try-with-resources`.

**Before/After**

```java
// Before – InputStream not in try-with-resources
InputStream inputStream = rlm.storageManager().fetchIndex(...);
try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, ...))) { ... }

// After
try (InputStream inputStream = rlm.storageManager().fetchIndex(...);
     BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, ...))) { ... }

// Before – stream passed directly, not closed on exception
Files.copy(rlm.storageManager().fetchIndex(...), tmpSnapshotFile, ...);

// After
try (InputStream inputStream = rlm.storageManager().fetchIndex(...)) {
    Files.copy(inputStream, tmpSnapshotFile, ...);
}
```

**How to spot**: Any `InputStream` returned from an external resource manager must be owned by a `try-with-resources` block at the call site. Wrapping it in another `Closeable` (e.g. `InputStreamReader`) that is itself try-with-resources only does not protect the underlying stream if the wrapper constructor throws.

**Test**: No dedicated test for the leak path; related `LocalTieredStorageTest` helper methods were updated to use `try-with-resources` for consistency.
