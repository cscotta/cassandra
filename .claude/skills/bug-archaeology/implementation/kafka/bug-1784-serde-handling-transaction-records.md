**Commit**: 3fafa096b1 · **Ticket**: KAFKA-18207
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    implementation project(':transaction-coordinator')
import org.apache.kafka.common.record.{Record, RecordBatch}

  /**
   * Exposed for printing records using [[kafka.tools.DumpLogSegments]]
   */
  def formatRecordKeyAndValue(record: Record): (Option[String], Option[String]) = {
    TransactionLog.readTxnRecordKey(record.key) match {
      case txnKey: TxnKey =>
        val keyString = s"transaction_metadata::transactionalId=${txnKey.transactionalId}"

        val valueString = TransactionLog.readTxnRecordValue(txnKey.transactionalId, record.value) match {

// After
    implementation project(':coordinator-common')
            <allow pkg="org.apache.kafka.coordinator.common.runtime" />
import org.apache.kafka.common.record.RecordBatch
import org.apache.kafka.coordinator.transaction.TransactionCoordinatorRecordSerde
import org.apache.kafka.coordinator.transaction.generated.{TransactionLogKey, TransactionLogKeyJsonConverter, TransactionLogValue, TransactionLogValueJsonConverter}
  // Package private for testing.
  class TransactionLogMessageParser extends MessageParser[String, String] {
    private val serde = new TransactionCoordinatorRecordSerde()

    private def prepareKey(message: Message, version: Short): String = {
      val messageAsJson = message match {
        case m: TransactionLogKey =>
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18207: serde for handling transaction records (#18136).

**Test**
Test modified in TransactionLogTest.scala covering the corrected behaviour.
