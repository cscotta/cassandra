**Commit**: 999afbbbf1 · **Ticket**: KAFKA-19069
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
## Description:
Add case for dumping LEADER_CHANGE record in DumpLogSegments. - Extract the part of printing ControlRecord as a method. - Add the unit test for printing the control record in dumpLog. ## Verification

### Before:
```
$ bin/kafka-dump-log.sh --cluster-metadata-decoder --files
try_KAFKA-19069/00000000000000000000.log
Dumping try_KAFKA-19069/00000000000000000000.log
Log starting offset: 0
baseOffset: 0 lastOffset: 2 count: 3 baseSequence: -1 lastSequence: -1
producerId: -1 producerEpoch: -1 partitionLeaderEpoch: 1
isTransactional: false isControl: true deleteHorizonMs:
OptionalLong.empty position: 0 CreateTime: 1747299887693 size: 174
magic: 2 compresscodec: none crc: 3714449884...

**Before/After**
```java
// Before
                val controlTypeId = ControlRecordType.parseTypeId(record.key)
                ControlRecordType.fromTypeId(controlTypeId) match {
                  case ControlRecordType.ABORT | ControlRecordType.COMMIT =>
                    val endTxnMarker = EndTransactionMarker.deserialize(record)
                    print(s" endTxnMarker: ${endTxnMarker.controlType} coordinatorEpoch: ${endTxnMarker.coordinatorEpoch}")
                  case ControlRecordType.SNAPSHOT_HEADER =>
                    val header = ControlRecordUtils.deserializeSnapshotHeaderRecord(record)
                    print(s" SnapshotHeader ${SnapshotHeaderRecordJsonConverter.write(header, header.version())}")
                  case ControlRecordType.SNAPSHOT_FOOTER =>
                    val footer = ControlRecordUtils.deserializeSnapshotFooterRecord(record)
                    print(s" SnapshotFooter ${SnapshotFooterRecordJsonConverter.write(footer, footer.version())}")
                  case ControlRecordType.KRAFT_VERSION =>

// After
import org.apache.kafka.common.message.LeaderChangeMessageJsonConverter
                printControlRecord(record)
  private def printControlRecord(record: Record): Unit = {
    val controlTypeId = ControlRecordType.parseTypeId(record.key)
    ControlRecordType.fromTypeId(controlTypeId) match {

```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19069 dumplogsegments does not dump the leader_change record (#19729).

**Test**
Test modified in DumpLogSegmentsTest.scala covering the corrected behaviour.
