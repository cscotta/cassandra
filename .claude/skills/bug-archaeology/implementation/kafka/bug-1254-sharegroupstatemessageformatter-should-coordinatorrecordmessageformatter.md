**Commit**: 5982943267 · **Ticket**: KAFKA-18409
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
ShareGroupStateMessageFormatter should extend
CoordinatorRecordMessageFormatter in order to have a consistent handling
of records of coordinators. Reviewers: Ken Huang <s7133700@gmail.com>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.MessageFormatter;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.coordinator.share.generated.CoordinatorRecordType;
import org.apache.kafka.coordinator.share.generated.ShareSnapshotKey;
import org.apache.kafka.coordinator.share.generated.ShareSnapshotKeyJsonConverter;
import org.apache.kafka.coordinator.share.generated.ShareSnapshotValue;
import org.apache.kafka.coordinator.share.generated.ShareSnapshotValueJsonConverter;
import org.apache.kafka.coordinator.share.generated.ShareUpdateKey;
import org.apache.kafka.coordinator.share.generated.ShareUpdateKeyJsonConverter;
import org.apache.kafka.coordinator.share.generated.ShareUpdateValue;

// After
import org.apache.kafka.coordinator.share.ShareCoordinatorRecordSerde;
import org.apache.kafka.coordinator.share.generated.CoordinatorRecordJsonConverters;
public class ShareGroupStateMessageFormatter extends CoordinatorRecordMessageFormatter {
    public ShareGroupStateMessageFormatter() {
        super(new ShareCoordinatorRecordSerde());
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18409: sharegroupstatemessageformatter should use coordinatorrecordmessageformatter (#18510).

**Test**
Test modified in ShareGroupStateMessageFormatterTest.java covering the corrected behaviour.
