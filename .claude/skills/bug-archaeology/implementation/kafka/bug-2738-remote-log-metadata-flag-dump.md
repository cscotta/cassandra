**Commit**: 8491e84aad · **Ticket**: KAFKA-16228
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
This change adds the --remote-log-metadata-decoder flag to the kafka-dump-log.sh tool. This new flag can be used to decode the payload of the __remote_log_metadata records produced by the default RemoteLogMetadataManager. Reviewers: Luke Chen <showuon@gmail.com>, Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before

    private val printOpt = parser.accepts("print-data-log", "if set, printing the messages content when dumping data logs. Automatically set if any decoder option is specified.")
    private val verifyOpt = parser.accepts("verify-index-only", "if set, just verify the index log without printing its content.")
    private val indexSanityOpt = parser.accepts("index-sanity-check", "if set, just checks the index sanity without printing its content. " +
    private val deepIterationOpt = parser.accepts("deep-iteration", "if set, uses deep instead of shallow iteration. Automatically set if print-data-log is enabled.")
    private val valueDecoderOpt = parser.accepts("value-decoder-class", "if set, used to deserialize the messages. This class should implement org.apache.kafka.tools.api.Decoder trait. Custom jar should be available in kafka/libs directory.")
    private val keyDecoderOpt = parser.accepts("key-decoder-class", "if set, used to deserialize the keys. This class should implement org.apache.kafka.tools.api.Decoder trait. Custom jar should be available in kafka/libs directory.")
    private val offsetsOpt = parser.accepts("offsets-decoder", "if set, log data will be parsed as offset data from the " +
    private val transactionLogOpt = parser.accepts("transaction-log-decoder", "if set, log data will be parsed as " +
    private val clusterMetadataOpt = parser.accepts("clu

// After
import org.apache.kafka.server.log.remote.metadata.storage.serialization.RemoteLogMetadataSerde
  private class RemoteMetadataLogMessageParser extends MessageParser[String, String] {
    private val metadataRecordSerde = new RemoteLogMetadataSerde

    override def parse(record: Record): (Option[String], Option[String]) = {
      val output = try {
        val data = new Array[Byte](record.value.remaining)
        record.value.get(data)
        metadataRecordSerde.deserialize(data).toString
      } catch {
        case e: Throwable =>
          s"Error at offset ${record.offset}, skipping. ${e.getMessage}"
```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16228: add remote log metadata flag to the dump log tool (#16475).

**Test**
Test modified in DumpLogSegmentsTest.scala covering the corrected behaviour.
