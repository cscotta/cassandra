**Commit**: a8f49999cc · **Ticket**: KAFKA-19019
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR adds the support for remote storage fetch for share groups. There is a limitation in remote storage fetch for consumer groups that
we can only perform remote fetch for a single topic partition in a fetch
request. Since, the logic of share fetch requests is largely based on
how consumer groups work, we are following similar logic in implementing 
remote storage fetch. However, this problem should be addressed as 
part of KAFKA-19133 which should help us perform fetch for multiple 
remote fetch topic partition in a single share fetch request.

**Before/After**
```java
// Before
    private LinkedHashMap<TopicIdPartition, LogReadResult> partitionsAlreadyFetched;
            time
        Time time
        this.partitionsAlreadyFetched = new LinkedHashMap<>();

// After
import org.apache.kafka.common.errors.KafkaStorageException;
import org.apache.kafka.common.errors.NotLeaderOrFollowerException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.message.ShareFetchResponseData;
import org.apache.kafka.server.storage.log.FetchPartitionData;
import org.apache.kafka.storage.internals.log.FetchDataInfo;
import org.apache.kafka.storage.internals.log.RemoteLogReadResult;
import org.apache.kafka.storage.internals.log.RemoteStorageFetchInfo;
import java.util.LinkedHashSet;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19019: add support for remote storage fetch for share groups (#19437).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
