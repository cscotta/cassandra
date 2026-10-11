**Commit**: 7b6e94642a · **Ticket**: KAFKA-18303
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Following https://github.com/apache/kafka/pull/18261, this patch updates the Share Coordinator to use the new record format. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
             "-m", "MessageDataGenerator", "JsonConverterGenerator"
import org.apache.kafka.coordinator.share.generated.{ShareSnapshotKey, ShareSnapshotValue, ShareUpdateKey, ShareUpdateValue}
import org.apache.kafka.coordinator.share.{ShareCoordinator, ShareCoordinatorRecordSerde}
          ShareCoordinator.SHARE_SNAPSHOT_RECORD_KEY_VERSION),
          ShareCoordinator.SHARE_SNAPSHOT_RECORD_VALUE_VERSION)
          ShareCoordinator.SHARE_UPDATE_RECORD_KEY_VERSION),
          0.toShort
    short SHARE_SNAPSHOT_RECORD_KEY_VERSION = 0;
    short SHARE_SNAPSHOT_RECORD_VALUE_VERSION = 0;
    short SHARE_UPDATE_RECORD_KEY_VERSION = 1;
    short SHARE_UPDATE_RECORD_VALUE_VERSION = 1;


// After
             "-m", "MessageDataGenerator", "JsonConverterGenerator",
             "-t", "CoordinatorRecordTypeGenerator"
import org.apache.kafka.coordinator.share.generated.{CoordinatorRecordType, ShareSnapshotKey, ShareSnapshotValue, ShareUpdateKey, ShareUpdateValue}
import org.apache.kafka.coordinator.share.ShareCoordinatorRecordSerde
          CoordinatorRecordType.SHARE_SNAPSHOT.id()),
          0.toShort)
          CoordinatorRecordType.SHARE_UPDATE.id()),
          CoordinatorRecordType.SHARE_SNAPSHOT.id()
import org.apache.kafka.coordinator.share.generated.CoordinatorRecordType;
                CoordinatorRecordType.SHARE_SNAPSHOT.id()),
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18303; update sharecoordinator to use new record format (#18396).

**Test**
Test modified in DumpLogSegmentsTest.scala covering the corrected behaviour.
