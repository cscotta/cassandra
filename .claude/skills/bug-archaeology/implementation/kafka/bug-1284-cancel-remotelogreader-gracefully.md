**Commit**: f24945b519 · **Ticket**: KAFKA-15931
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reverts commit
https://github.com/jeqo/kafka/commit/2723dbf3a007af0a1cafc17ed019aabd46eb6881
and
https://github.com/jeqo/kafka/commit/269e8892ad26904c82a3bff7b7d7a81c47619e4a. Instead of reopening the transaction index, it cancels the
RemoteFetchTask without interrupting it--avoiding to close the
TransactionIndex channel. This will lead to complete the execution of the remote fetch but ignoring
the results. Given that this is considered a rare case, we could live
with this.

**Before/After**
```java
// Before
    // cancel the remote storage read task, if it has not been executed yet
    val cancelled = remoteFetchTask.cancel(true)
    verify(remoteFetchTask).cancel(true)
            // If txn index does not exist on the source, it's an empty file on the index entry
                return offsetIndex.sizeInBytes() + timeIndex.sizeInBytes() + Files.size(txnIndex.path());
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
    private static final Logger log = LoggerFactory.getLogger(TransactionIndex.class);

    final TransactionIndexFile txnFile;

// After
    // cancel the remote storage read task, if it has not been executed yet and
    // avoid interrupting the task if it is already running as it may force closing opened/cached resources as transaction index.
    val cancelled = remoteFetchTask.cancel(false)
    verify(remoteFetchTask).cancel(false)
                return offsetIndex.sizeInBytes() + timeIndex.sizeInBytes() + Files.size(txnIndex.file().toPath());
import java.util.Optional;
import java.util.OptionalLong;
    private volatile File file;
    // note that the file is not created until we need it
    private Optional<FileChannel> maybeChannel = Optional.empty();
    private OptionalLong lastOffset = OptionalLong.empty();
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15931: cancel remotelogreader gracefully (#19197).

**Test**
Test modified in DelayedRemoteFetchTest.scala covering the corrected behaviour.
