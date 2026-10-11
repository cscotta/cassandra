**Commit**: 4fd0f4095e · **Ticket**: KAFKA-17118
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

  def buildMetadataProperties(
    clusterIdStr: String,
    config: KafkaConfig
  ): MetaProperties = {
    val effectiveClusterId = try {
      Uuid.fromString(clusterIdStr)
    } catch {
      case e: Throwable => throw new TerseFailure(s"Cluster ID string $clusterIdStr " +
        s"does not appear to be a valid UUID: ${e.getMessage}")
    }
    if (config.nodeId < 0) {

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17118 remove useless code in storagetool (#16622).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
