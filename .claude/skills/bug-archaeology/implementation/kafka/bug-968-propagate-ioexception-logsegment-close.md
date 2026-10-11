**Commit**: edd0efdebf · **Ticket**: KAFKA-19221
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Log segment closure results in right sizing the segment on disk along
with the associated index files. This is specially important for TimeIndexes where a failure to right
size may eventually cause log roll failures leading to under replication
and log cleaner failures. This change uses `Utils.closeAll` which propagates exceptions, resulting
in an "unclean" shutdown. That would then cause the broker to attempt to
recover the log segment and the index on next startup, thereby avoiding
the failures described above.

**Before/After**
```java
// Before
// (no removals)

// After
        if (!channel.isOpen()) {
            return;
        }

import org.apache.kafka.storage.internals.checkpoint.CleanShutdownFileHandler;
import java.io.File;
    @ClusterTest(types = {Type.KRAFT})
    public void testIOExceptionOnLogSegmentCloseResultsInRecovery() throws IOException, InterruptedException, ExecutionException {
        try (Admin admin = cluster.admin()) {
            admin.createTopics(List.of(new NewTopic("foo", 1, (short) 1))).all().get();
        }
        cluster.waitForTopic("foo", 1);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19221 propagate ioexception on logsegment#close (#19607).

**Test**
Test modified in LogManagerIntegrationTest.java covering the corrected behaviour.
