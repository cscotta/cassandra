# Bug 0929: Out-of-Space Error Swallowed in Memtable Flushing

**Commit**: f3b3c410a0 · **Ticket**: CASSANDRA-11448
**Subsystem**: Memtable / Storage engine
**Category**: Logic error in condition

---

## Root Cause
When all data directories were blacklisted as unwritable, `Directories.getWriteableLocation()` threw a plain `IOError` (and `DiskAwareRunnable.getWriteDirectory()` threw a `RuntimeException`) instead of the well-typed `FSWriteError`. Because `FSWriteError` was not thrown, the `JVMStabilityInspector` never saw the disk-failure event, and—critically—the `PostFlush` task still called `CommitLog.discardCompletedSegments()`, discarding commit-log data even though the corresponding memtable was never durably flushed to disk. The fix changes both throw sites to emit `FSWriteError`, wraps the flush loop in a try-catch that captures it in `postFlush.flushFailure`, prevents commit-log discard when a flush failure occurred, and re-throws the error after metrics are updated.

### Before / After
```java
// BEFORE (Directories.java) — wrong exception type, never caught as FSWriteError
throw new IOError(new IOException("All configured data directories have been blacklisted..."));

// BEFORE (DiskAwareRunnable.java) — completely wrong type
throw new RuntimeException("Insufficient disk space to write " + writeSize + " bytes");

// BEFORE (PostFlush.run) — commit log discarded even when flush failed
if (lastReplayPosition != null)
    CommitLog.instance.discardCompletedSegments(metadata.cfId, lastReplayPosition);

// AFTER — typed correctly, commit log protected
throw new FSWriteError(new IOException("..."), "");

if (lastReplayPosition != null && flushFailure == null)
    CommitLog.instance.discardCompletedSegments(metadata.cfId, lastReplayPosition);

if (flushFailure != null)
    throw flushFailure;
```

---

## How to Spot
Any code that catches `FSWriteError` to trigger disk-failure handling or to protect commit-log discard must ensure that all throw sites in the flush pipeline emit `FSWriteError`, not a base `IOException`, `IOError`, or `RuntimeException`. During review, verify every path that can fail due to disk fullness throws the correct typed error, and that commit-log advancement is gated on flush success.

---

## Test
A new `OutOfSpaceTest` was added (`src/test/org/apache/cassandra/cql3/OutOfSpaceTest.java`) that injects a disk-full condition and verifies the commit log is not discarded and the error is properly propagated.
