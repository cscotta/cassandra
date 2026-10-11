**Commit**: 6b96273ba0 · **Ticket**: KAFKA-19345
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch wraps ShareFetchUtils static method `processFetchResponse`
with MockedStatic  to improve test isolation and also fixes some
incorrect test results. Reviewers: Abhinav Dixit <adixit@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before

import org.apache.kafka.server.storage.log.FetchPartitionData;
import static org.apache.kafka.server.share.fetch.ShareFetchTestUtils.createShareAcquiredRecords;
import static org.mockito.ArgumentMatchers.anyString;
        when(sp0.acquire(any(), anyInt(), anyInt(), anyLong(), any(), any())).thenReturn(
            createShareAcquiredRecords(new ShareFetchResponseData.AcquiredRecords().setFirstOffset(0).setLastOffset(3).setDeliveryCount((short) 1)));
        // the fetchOffsetMetadata will return empty. Post the readFromLog call, the fetchOffsetMetadata will be
        mockTopicIdPartitionFetchBytes(replicaManager, tp0, hwmOffsetMetadata);
        // Since sp1 cannot be acquired, tryComplete should return false.
        Mockito.verify(exceptionHandler, times(1)).accept(any(), any());

// After
import static org.mockito.Mockito.never;
        // the fetchOffsetMetadata will return empty. Post the first readFromLog call, the fetchOffsetMetadata will be
        mockTopicIdPartitionFetchBytes(hwmOffsetMetadata, p0);
        // Since minBytes(2) is not satisfied (only 1 byte available from sp0), and sp1 cannot be acquired, tryComplete should return false.
        Mockito.verify(exceptionHandler, never()).accept(any(), any());
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19345: use sharefetchutils mock for delayedsharefetchtest tests (#20765).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
