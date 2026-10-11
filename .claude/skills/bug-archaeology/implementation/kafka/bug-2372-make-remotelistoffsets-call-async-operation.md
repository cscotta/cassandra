**Commit**: 344d8a60af · **Ticket**: KAFKA-15859
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
This is the part-2 of the KIP-1075

To find the offset for a given timestamp, ListOffsets API is used by the client. When the topic is enabled with remote storage, then we have to fetch the remote indexes such as offset-index and time-index to serve the query. Also, the ListOffsets request can contain the query for multiple topics/partitions. The time taken to read the indexes from remote storage is non-deterministic and the query is handled by the request-handler threads.

**Before/After**
```java
// Before
// (no removals)

// After
import kafka.log.AsyncOffsetReadFutureHolder;
import kafka.server.DelayedOperationPurgatory;
import kafka.server.DelayedRemoteListOffsets;
import kafka.server.TopicPartitionOperationKey;
import java.util.function.Supplier;
import scala.util.Either;
    private DelayedOperationPurgatory<DelayedRemoteListOffsets> delayedRemoteListOffsetsPurgatory;
    public void setDelayedOperationPurgatory(DelayedOperationPurgatory<DelayedRemoteListOffsets> delayedRemoteListOffsetsPurgatory) {
        this.delayedRemoteListOffsetsPurgatory = delayedRemoteListOffsetsPurgatory;
    }

    public AsyncOffsetReadFutureHolder<Either<Exception, Option<FileRecords.TimestampAndOffset>>> asyncOffsetRead(
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15859 make remotelistoffsets call an async operation (#16602).

**Test**
Test modified in RemoteLogOffsetReaderTest.java covering the corrected behaviour.
