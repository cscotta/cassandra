**Commit**: b6adec48c5 · **Ticket**: KAFKA-18616
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
This patch refactors the `ApiMessageFormatter` to follow what we have
done in https://github.com/apache/kafka/pull/18688. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
public abstract class ApiMessageFormatter implements MessageFormatter {

    static final String UNKNOWN = "unknown";
        byte[] key = consumerRecord.key();
        if (Objects.nonNull(key)) {
            short keyVersion = ByteBuffer.wrap(key).getShort();
            JsonNode dataNode = readToKeyJson(ByteBuffer.wrap(key));
            if (dataNode instanceof NullNode) {
                return;
            json.putObject(KEY)
                    .put(TYPE, keyVersion)
                    .set(DATA, dataNode);

// After
      <allow pkg="org.apache.kafka.coordinator.common.runtime" />
import org.apache.kafka.common.protocol.ApiMessage;
import org.apache.kafka.coordinator.common.runtime.CoordinatorRecord;
import org.apache.kafka.coordinator.common.runtime.CoordinatorRecordSerde;
public abstract class CoordinatorRecordMessageFormatter implements MessageFormatter {

    private final CoordinatorRecordSerde serde;

    public CoordinatorRecordMessageFormatter(CoordinatorRecordSerde serde) {
        this.serde = serde;
    }
        if (Objects.isNull(consumerRecord.key())) return;
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18616; refactor tools's apimessageformatter (#18695).

**Test**
Test modified in CoordinatorRecordMessageFormatterTest.java covering the corrected behaviour.
