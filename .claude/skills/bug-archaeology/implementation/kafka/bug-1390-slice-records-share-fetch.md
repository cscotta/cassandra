**Commit**: 48a506b7b8 · **Ticket**: KAFKA-18522
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR handles slicing of fetched records based on acquire response for
share fetch. There could be additional bytes fetched from log but
acquired offsets can be a subset, typically with `max fetch records`
configuration. Rather sending additional bytes of fetched data to client
we should slice the file and wire only needed batches. Note: If the acquired offsets are within a batch then we need to send
the entire batch within the file record.

**Before/After**
```java
// Before
                        // We set the records to the fetchPartitionData records. We do not alter the records
                        // fetched from the replica manager as they follow zero copy buffer. The acquired records
                        // might be a subset of the records fetched from the replica manager, depending
                        // on the max fetch records or available records in the share partition. The client
                        // sends the max bytes in request which should limit the bytes sent to the client
                        // in the response.
                        .setRecords(fetchPartitionData.records)

// After
import org.apache.kafka.common.message.ShareFetchResponseData.AcquiredRecords;
import org.apache.kafka.common.record.FileLogInputStream.FileChannelRecordBatch;
import org.apache.kafka.common.record.Records;
import java.util.Iterator;
                        .setRecords(maybeSliceFetchRecords(fetchPartitionData.records, shareAcquiredRecords))

    /**
     * Slice the fetch records based on the acquired records. The slicing is done based on the first
     * and last offset of the acquired records from the list. The slicing doesn't consider individual
     * acquired batches rather the boundaries of the acquired list. The method expects the acquired
     * records list to be within the fetch records bounds.
     *
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18522: slice records for share fetch (#18804).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
