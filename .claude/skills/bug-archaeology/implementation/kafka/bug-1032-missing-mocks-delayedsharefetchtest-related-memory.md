**Commit**: 14ed1162a5 · **Ticket**: KAFKA-19310
**Subsystem**: core/src
**Category**: Silent skip on non-match

**Root cause**
### About
Added test memory records to avoid the silent exception thrown during
slicing. ### Testing
Ran the tests of `DelayedShareFetchTest` to make sure that there is no
silent exception in any test. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
        when(sp0.acquire(any(), anyInt(), anyInt(), anyLong(), any(), any())).thenReturn(
            createShareAcquiredRecords(new ShareFetchResponseData.AcquiredRecords().setFirstOffset(0).setLastOffset(3).setDeliveryCount((short) 1)));
        when(sp1.acquire(any(), anyInt(), anyInt(), anyLong(), any(), any())).thenReturn(
            createShareAcquiredRecords(new ShareFetchResponseData.AcquiredRecords().setFirstOffset(0).setLastOffset(3).setDeliveryCount((short) 1)));
        when(sp0.acquire(any(), anyInt(), anyInt(), anyLong(), any(), any())).thenReturn(
            createShareAcquiredRecords(new ShareFetchResponseData.AcquiredRecords().setFirstOffset(0).setLastOffset(3).setDeliveryCount((short) 1)));
        when(sp0.acquire(any(), anyInt(), anyInt(), anyLong(), any(), any())).thenReturn(
            createShareAcquiredRecords(new ShareFetchResponseData.AcquiredRecords().setFirstOffset(0).setLastOffset(3).setDeliveryCount((short) 1)));
        when(sp1.acquire(any(), anyInt(), anyInt(), anyLong(), any(), any())).thenReturn(
            createShareAcquiredRecords(new ShareFetchResponseData.AcquiredRecords().setFirstOffset(0).setLastOffset(3).setDeliveryCount((short) 1)));
        when(sp2.acquire(any(), anyInt(), anyInt(), anyLong(), any(), any())).thenReturn(
            createShareAcquiredRecords(new ShareFetchResponseData.AcquiredRecords().setFirstOffset(0).setLastOffset(3).setDeliveryCount((short) 1)));

// After
import org.mockito.MockedStatic;
        MockedStatic<ShareFetchUtils> mockedShareFetchUtils = Mockito.mockStatic(ShareFetchUtils.class);
        Map<TopicIdPartition, ShareFetchResponseData.PartitionData> partitionDataMap = new LinkedHashMap<>();
        partitionDataMap.put(tp0, mock(ShareFetchResponseData.PartitionData.class));
        partitionDataMap.put(tp1, mock(ShareFetchResponseData.PartitionData.class));
        mockedShareFetchUtils.when(() -> ShareFetchUtils.processFetchResponse(any(), any(), any(), any(), any())).thenReturn(partitionDataMap);
        mockedShareFetchUtils.close();
        MockedStatic<ShareFetchUtils> mockedShareFetchUtils = Mockito.mockStatic(ShareFetchUtils.class);
        Map<TopicIdPartition, ShareFetchResponseData.PartitionData> partitionDataMap = new LinkedHashMap<>();
        partitionDataMap.put(tp0, mock(ShareFetchResponseData.PartitionData.class));
        mockedShareFetchUtils.when(() -> ShareFetchUtils.processFetchResponse(any(), any(), any(), any(), any())).thenReturn(partitionDataMap);
        mockedShareFetchUtils.close();
```

**How to spot**
When reviewing core/src code, look for silent skip on non-match patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19310: (minor) missing mocks for delayedsharefetchtest tests related to memory records slicing (#19823).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
