**Commit**: 571f50817c · **Ticket**: KAFKA-17411
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Instead of waiting until Tasks are assigned to us, we pre-emptively
create a StandbyTask for each non-empty Task directory found on-disk. We do this before starting any StreamThreads, and on our first
assignment (after joining the consumer group), we recycle any of these
StandbyTasks that were assigned to us, either as an Active or a
Standby. We can't just use these "initial Standbys" as-is, because they were
constructed outside the context of a StreamThread, so we first have to
update them with the context (log context, ChangelogReader, and source
topics) of the thread that it has been assigned to. The motivation for this is to (in a later commit) read StateStore
offsets for unowned Tasks f...

**Before/After**
```java
// Before
              files="(KafkaStreams|KStreamImpl|KTableImpl|InternalTopologyBuilder|StreamsPartitionAssignor|StreamThread|IQv2StoreIntegrationTest|KStreamImplTest|RocksDBStore|StreamTask).java"/>
        final LogContext logContext = new LogContext(String.format("stream-client [%s] ", clientId));
    private final String logPrefix;

// After
              files="(KafkaStreams|KStreamImpl|KTableImpl|InternalTopologyBuilder|StreamsPartitionAssignor|StreamThread|IQv2StoreIntegrationTest|KStreamImplTest|RocksDBStore|StreamTask|TaskManager).java"/>
    private final LogContext logContext;
            // all (alive) threads have received their assignment, close any remaining startup tasks, they're not needed
            stateDirectory.closeStartupTasks();

        logContext = new LogContext(String.format("stream-client [%s] ", clientId));
            log.debug("Initializing STANDBY tasks for existing local state");
            stateDirectory.initializeStartupTasks(topologyMetadata, streamsMetrics, logContext);

import java.util.HashSet;
    private String logPrefix;
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17411: create local state standbys on start (#16922).

**Test**
Test modified in KafkaStreamsTest.java covering the corrected behaviour.
