**Commit**: a0f6e6f816 · **Ticket**: KAFKA-16192
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This change includes adding transaction.version (part of KIP-1022)

New transaction version 1 is introduced to support writing flexible fields in transaction state log messages. Transaction version 2 is created in anticipation for further KIP-890 changes. Neither are made production ready. Tests for the new transaction version and new MV are created.

**Before/After**
```java
// Before
    val txnStateManager = new TransactionStateManager(config.brokerId, scheduler, replicaManager, txnConfig,
  private[transaction] def valueToBytes(txnMetadata: TxnTransitMetadata): Array[Byte] = {
    // Serialize with the highest supported non-flexible version
    // until a tagged field is introduced or the version is bumped.
    MessageUtil.toVersionPrefixedBytes(0,
import kafka.server.{ReplicaManager, RequestLocal}

// After
    val txnStateManager = new TransactionStateManager(config.brokerId, scheduler, replicaManager, metadataCache, txnConfig,
  private[transaction] def valueToBytes(txnMetadata: TxnTransitMetadata,
                                        usesFlexibleRecords: Boolean): Array[Byte] = {
    // Serialize with version 0 (highest non-flexible version) until transaction.version 1 is enabled
    // which enables flexible fields in records.
    val version: Short =
      if (usesFlexibleRecords) 1 else 0
    MessageUtil.toVersionPrefixedBytes(version,
import kafka.server.{MetadataCache, ReplicaManager, RequestLocal}
import org.apache.kafka.server.common.TransactionVersion
                              metadataCache: MetadataCache,
  private[transaction] def usesFlexibleRecords(): Boolean = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16192: introduce transaction.version and usage of flexible records to coordinators (#16183).

**Test**
Test modified in QuorumTestHarness.scala covering the corrected behaviour.
