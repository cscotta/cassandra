**Commit**: 0049b967e5 · **Ticket**: KAFKA-17890
**Subsystem**: checkstyle/import-control-server-common.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Jun Rao <jun@confluent.io>, Apoorv Mittal <amittal@confluent.io>

**Before/After**
```java
// Before
import kafka.server.DelayedOperationPurgatory;
import kafka.server.TopicPartitionOperationKey;
import kafka.server.DelayedOperationPurgatory;
import kafka.server.DelayedOperation;
import scala.Option;
        super(shareFetchData.fetchParams().maxWaitMs, Option.empty());
import kafka.server.DelayedOperationKey;

public class DelayedShareFetchGroupKey impl

// After
        <subpackage name="purgatory">
            <allow pkg="org.apache.kafka.server.metrics" />
            <allow pkg="org.apache.kafka.server.util" />
        </subpackage>

import org.apache.kafka.server.purgatory.DelayedOperationPurgatory;
import org.apache.kafka.server.purgatory.TopicPartitionOperationKey;
import org.apache.kafka.server.purgatory.DelayedOperationPurgatory;
import org.apache.kafka.server.purgatory.DelayedOperation;
        super(shareFetchData.fetchParams().maxWaitMs, Optional.empty());
```

**How to spot**
When reviewing checkstyle/import-control-server-common.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17890: move delayedoperationpurgatory to server-common (#17636).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
