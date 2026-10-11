**Commit**: a056672f7c · **Ticket**: KAFKA-19599
**Subsystem**: storage/src
**Category**: Resource leak

**Root cause**
During broker restarts, the topic-based RemoteLogMetadataManager (RLMM)
constructs the state by reading the internal `__remote_log_metadata`
topic. When the partition is not ready to perform remote storage
operations, then ReplicaNotAvailableException thrown back to the
consumer. The clients retries the request immediately. This results in a lot of FETCH requests on the broker and utilizes the
request handler threads.

**Before/After**
```java
// Before
            // Throwing a retriable ReplicaNotAvailableException here for clients retry.
            throw new ReplicaNotAvailableException("Remote log metadata cache is not initialized for partition: " + topicIdPartition);

// After
import java.util.concurrent.TimeUnit;
    boolean awaitInitialized(long timeout, TimeUnit unit) throws InterruptedException {
        return initializedLatch.await(timeout, unit);
    }

import java.util.concurrent.TimeUnit;
            try {
                boolean initialized = remoteLogMetadataCache.awaitInitialized(100, TimeUnit.MILLISECONDS);
                if (!initialized) {
                    // Throwing a retriable ReplicaNotAvailableException here for clients retry.
                    throw new ReplicaNotAvailableException("Remote log metadata cache is not initialized for partition: " + topicIdPartition);
                }
```

**How to spot**
When reviewing storage/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19599: reduce the frequency of replicanotavailableexception thrown to clients when rlmm is not ready (#20345).

**Test**
Test modified in RemoteLogMetadataCacheTest.java covering the corrected behaviour.
