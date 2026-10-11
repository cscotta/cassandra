**Commit**: cfd18132e8 · **Ticket**: KAFKA-19328
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Hi, I've created pull request. jira: [19328](https://issues.apache.org/jira/browse/KAFKA-19328)

problem:

1. doAnswer chaining works as intended only when calls are made
sequentially. In a multithreaded environment, its behavior is
unpredictable.

**Before/After**
```java
// Before
import org.apache.kafka.server.storage.log.FetchPartitionData;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.apache.kafka.server.share.fetch.ShareFetchTestUtils.createShareAcquiredRecords;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
    @Test
    public void testMultipleConcurrentShareFetches() throws InterruptedException {
        String groupId = "grp";
        Uuid memberId1 = Uuid.randomUuid();
        Uuid fooId = Uuid.randomUuid();

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19328: sharepartitionmanagertest testmultipleconcurrentsharefetches doanswer chaining needs verification (#19872).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
