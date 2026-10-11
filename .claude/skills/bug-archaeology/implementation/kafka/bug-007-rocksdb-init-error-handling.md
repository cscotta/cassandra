**Commit**: 794d2b625f · **Ticket**: KAFKA-20396
**Subsystem**: streams — RocksDBStore / state store initialization
**Category**: Resource leak

**Root cause**
When `RocksDBStore.openDB()` encountered an invalid state under EOS (e.g., a store with uncommitted data after a crash), it threw a `StreamsException`, which was caught and rethrown as a `ProcessorStateException`. However, `ProcessorStateException` is not handled by `ProcessorStateManager` as a signal to wipe and re-initialize the store — only `TaskCorruptedException` is. The fix catches `ProcessorStateException` from the EOS validity check and wraps it in `TaskCorruptedException` so that `ProcessorStateManager` properly clears the corrupted store. Additionally, the `taskId` was not set for segmented stores before `openDB()` was called on their segments, meaning the `TaskCorruptedException` could not identify which task was corrupted.

**Before/After**
```java
// Before
} catch (final StreamsException fatal) {
    final String fatalMessage = "State store " + name + " didn't find a valid state ...";
    throw new ProcessorStateException(fatalMessage, fatal);   // not caught as corruption
}

// After
} catch (final ProcessorStateException e) {
    final String message = "State store " + name + " didn't find a valid state ...";
    throw new TaskCorruptedException(Set.of(taskId), new ProcessorStateException(message, e));
}
```

**How to spot**
Any initialization path that detects corruption but throws a generic exception type that bypasses the corruption-recovery handler is susceptible. Always verify that EOS corruption paths throw the exact exception type that the state manager's recovery loop catches (`TaskCorruptedException` in Kafka Streams).

**Test**
`AbstractRocksDBSegmentedBytesStoreTest` — new test infrastructure (`setTaskId` plumbing) enables corruption-recovery tests for segmented stores. Existing recovery tests in `RocksDBStoreTest` verify the correct exception type is raised on EOS invalid-state detection.
