**Commit**: 80d2a8a42d · **Ticket**: KAFKA-18616
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
All the work that we have done to automate and to simplify the coordinator records allows us to simplify the related MessageParsers in DumpLogSegments. They can all share the same based implementation. This is nice because it ensures that we handle all those records similarly. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.generated.{CoordinatorRecordJsonConverters => GroupCoordinatorRecordJsonConverters, CoordinatorRecordType => GroupCoordinatorRecordType, GroupMetadataValue, GroupMetadataValueJsonConverter}
  // Package private for testing.
  class OffsetsMessageParser extends MessageParser[String, String] {
    private val serde = new GroupCoordinatorRecordSerde()
      json.set("data", GroupCoordinatorRecordJsonConverters.writeRecordKeyAsJson(message))

    private def prepareValue(message: ApiMessage, version: Short): String = {
      val messageAsJson = if (message.apiKey == GroupCoordinatorRecordType.GROUP_METADATA.id) {
        prepareGroupMetadataValue(message.asInstanceOf[GroupMetadataValue], version)
      } else {
        GroupCoordinatorRecordJsonConverters.writeRecordValueAsJson(message, version)
      }

// After
import org.apache.kafka.coordinator.group.generated.{GroupMetadataValue, GroupMetadataValueJsonConverter, CoordinatorRecordJsonConverters => GroupCoordinatorRecordJsonConverters, CoordinatorRecordType => GroupCoordinatorRecordType}
import org.apache.kafka.coordinator.common.runtime.CoordinatorRecordSerde
  abstract class CoordinatorRecordMessageParser(serde: CoordinatorRecordSerde) extends MessageParser[String, String] {
    override def parse(record: Record): (Option[String], Option[String]) = {
      if (!record.hasKey)
        throw new RuntimeException(s"Failed to decode message at offset ${record.offset} using the " +
          "specified decoder (message had a missing key)")

      try {
        val r = serde.deserialize(record.key, record.value)
        (
          Some(prepareKey(r.key)),
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18616; refactor dumplogsegments's messageparsers (#18688).

**Test**
Test modified in DumpLogSegmentsTest.scala covering the corrected behaviour.
