**Commit**: 94f5a4f63e · **Ticket**: KAFKA-17135
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.storage.internals.log.{AppendOrigin, CompletedTxn, LogFileUtils, LogOffsetMetadata, ProducerAppendInfo, ProducerStateEntry, ProducerStateManager, ProducerStateManagerConfig, TxnMetadata, VerificationStateEntry}
    private static void writeSnapshot(File file, Map<Long, ProducerStateEntry> entries, boolean sync) throws IOException {

// After
import org.apache.kafka.storage.internals.log.{AppendOrigin, BatchMetadata, CompletedTxn, LogFileUtils, LogOffsetMetadata, ProducerAppendInfo, ProducerStateEntry, ProducerStateManager, ProducerStateManagerConfig, TxnMetadata, VerificationStateEntry}
  @Test
  def testReadWriteSnapshot(): Unit = {
    val expectedEntryMap = new java.util.HashMap[java.lang.Long, ProducerStateEntry]
    expectedEntryMap.put(1, new ProducerStateEntry(1L, 2, 3, RecordBatch.NO_TIMESTAMP,
      OptionalLong.of(100L), java.util.Optional.of(new BatchMetadata(1, 2L, 3, RecordBatch.NO_TIMESTAMP))))
    expectedEntryMap.put(11, new ProducerStateEntry(11L, 12, 13, 123456L,
      OptionalLong.empty(), java.util.Optional.empty()))

    def assertEntries(actual: util.List[ProducerStateEntry]): Unit = {
      val actualEntryMap = actual.asScala.map(p => (p.producerId(), p)).toMap.asJava
      assertEquals(expectedEntryMap.keySet(), actualEntryMap.keySet())
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17135 add unit test for `producerstatemanager#readsnapshot` and `producerstatemanager#writesnapshot` (#16603).

**Test**
Test modified in ProducerStateManagerTest.scala covering the corrected behaviour.
