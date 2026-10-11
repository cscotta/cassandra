**Commit**: fe56fc98fa · **Ticket**: KAFKA-18269
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Included in this change:
1. Remove deprecated protocol api versions from json files. 3. Remove fields that are no longer used from json files (affects ListOffsets, OffsetCommit, DescribeConfigs).

**Before/After**
```java
// Before
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.requests.ProduceRequest;

import java.util.Optional;
    private byte maxUsableProduceMagic = RecordBatch.CURRENT_MAGIC_VALUE;
        this.maxUsableProduceMagic = computeMaxUsableProduceMagic();

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18269: remove deprecated protocol apis support (kip-896, kip-724) (#18218).

**Test**
Test modified in ApiVersionsTest.java covering the corrected behaviour.
