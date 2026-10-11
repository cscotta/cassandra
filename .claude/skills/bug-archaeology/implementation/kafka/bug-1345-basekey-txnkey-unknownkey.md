**Commit**: 18eca0229d · **Ticket**: KAFKA-18882
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>, PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    * @return the key
  def readTxnRecordKey(buffer: ByteBuffer): BaseKey = {
    if (version == CoordinatorRecordType.TRANSACTION_LOG.id) {
      val value = new TransactionLogKey(new ByteBufferAccessor(buffer), 0.toShort)
      TxnKey(
        version = version,
        transactionalId = value.transactionalId
      )
    } else {
      UnknownKey(version)
    }


// After
    * @return left with the version if the key is not a transaction log key, right with the transactional id otherwise
  def readTxnRecordKey(buffer: ByteBuffer): Either[Short, String] = {
    Either.cond(
      version == CoordinatorRecordType.TRANSACTION_LOG.id,
      new TransactionLogKey(new ByteBufferAccessor(buffer), 0.toShort).transactionalId,
      version
    )
                  case Left(version) =>
                    warn(s"Unknown message key with version $version" +
                      s" while loading transaction state from $topicPartition. Ignoring it. " +
                      "It could be a left over from an aborted upgrade.")
                  case Right(transactionalId) =>
```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18882 remove basekey, txnkey, and unknownkey (#19054).

**Test**
Test modified in TransactionLogTest.scala covering the corrected behaviour.
