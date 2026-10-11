**Commit**: 76bf38a4fd · **Ticket**: KAFKA-18604
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This patch updates the transaction coordinator record to use the new coordinator record definition. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
             "-m", "MessageDataGenerator", "JsonConverterGenerator"
     * @param recordVersion - short representing version
    protected abstract ApiMessage apiMessageValueFor(short recordVersion);
import org.apache.kafka.coordinator.transaction.generated.{TransactionLogKey, TransactionLogValue}
    MessageUtil.toVersionPrefixedBytes(TransactionLogKey.HIGHEST_SUPPORTED_VERSION,
    if (version >= TransactionLogKey.LOWEST_SUPPORTED_VERSION && version <= TransactionLogKey.HIGHEST_SUPPORTED_VERSION) {
      val value = new TransactionLogKey(new ByteBufferAccessor(buffer), version)
import org.apache.kafka.common.protocol.{ApiMessage, ByteBufferAccessor, Message}
import org.apache.kafka.coordinator.transaction.generated.{TransactionLogKey, TransactionLogKeyJsonConverter, TransactionLogValue, TransactionLogValueJsonConverter}

// After
             "-m", "MessageDataGenerator", "JsonConverterGenerator",
             "-t", "CoordinatorRecordTypeGenerator", "CoordinatorRecordJsonConvertersGenerator"
     * @param recordType - short representing type
    protected abstract ApiMessage apiMessageValueFor(short recordType);
import org.apache.kafka.coordinator.transaction.generated.{CoordinatorRecordType, TransactionLogKey, TransactionLogValue}
    MessageUtil.toCoordinatorTypePrefixedBytes(CoordinatorRecordType.TRANSACTION_LOG.id,
    if (version == CoordinatorRecordType.TRANSACTION_LOG.id) {
      val value = new TransactionLogKey(new ByteBufferAccessor(buffer), 0.toShort)
import org.apache.kafka.common.protocol.{ApiMessage, ByteBufferAccessor}
import org.apache.kafka.coordinator.transaction.generated.{CoordinatorRecordJsonConverters => TransactionCoordinatorRecordJsonConverters}
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18604; update transaction coordinator (#18636).

**Test**
No test added.
