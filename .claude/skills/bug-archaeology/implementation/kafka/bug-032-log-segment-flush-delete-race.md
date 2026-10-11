**Commit**: eaad6ed14a · **Ticket**: KAFKA-19571
**Subsystem**: core/src/main/scala/kafka/log/LogManager.scala
**Category**: Race condition / missing synchronization

**Root cause**
`LogManager.replaceCurrentWithFutureLog` called `srcLog.close()` immediately after renaming the source log directory to the `.delete` suffix, before the background delete-logs thread had removed the files. Concurrent operations such as the log flusher or an in-flight fetch could still hold references to the log's file channel; closing the log channel while those operations were in flight caused a `ClosedChannelException`, which would take the log directory offline.

**Before/After**
```scala
// Before
srcLog.renameDir(UnifiedLog.logDeleteDirName(topicPartition), true)
srcLog.close()   // races with concurrent flush/fetch

// After
srcLog.renameDir(UnifiedLog.logDeleteDirName(topicPartition), true)
// close() removed; Unix semantics keep renamed files accessible until
// all handles are closed; the background thread deletes them later.
```

**How to spot**
Calling `close()` on a shared resource immediately after renaming it for deferred deletion is unsafe when other threads still hold references. The safe pattern is to let the background cleanup thread close the resource, relying on OS file-handle reference counting.

**Test**
`LogManagerTest#testReplaceCurrentWithFutureLogDoesNotCloseSourceLog`: spies on the source log, calls `replaceCurrentWithFutureLog`, verifies `close()` is never called, and asserts that `flush(false)` on the renamed log succeeds without throwing.
