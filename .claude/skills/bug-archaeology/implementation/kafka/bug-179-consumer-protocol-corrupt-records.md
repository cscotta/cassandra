**Commit**: 5c01fd0b76 · **Ticket**: KAFKA-18949
**Subsystem**: core/src/test/scala/integration/kafka/api/PlaintextAdminIntegrationTest.scala
**Category**: State not cleaned up

**Root cause**: `testDeleteRecordsAfterCorruptRecords` was restricted to the classic consumer protocol only (`getTestGroupProtocolParametersClassicGroupProtocolOnly`), so the consumer protocol path was never exercised. Additionally, the test created a consumer and called `subscribeAndWaitForAssignment` before corrupting the log, then attempted `consumer.seekToBeginning` — this pattern is incompatible with the consumer protocol which handles seeks differently. The fix switches to `getTestGroupProtocolParametersAll`, removes the pre-corruption consumer, and instead creates a fresh consumer post-corruption with `auto.offset.reset=earliest`.

**Before/After**:
```scala
// BEFORE – classic-only, pre-creates consumer before corruption
@MethodSource(Array("getTestGroupProtocolParametersClassicGroupProtocolOnly"))
def testDeleteRecordsAfterCorruptRecords(groupProtocol: String): Unit = {
    val consumer = createConsumer()
    subscribeAndWaitForAssignment(topic, consumer)
    ...
    consumer.seekToBeginning(Collections.singletonList(topicPartition))

// AFTER – all protocols, fresh consumer after corruption
@MethodSource(Array("getTestGroupProtocolParametersAll"))
def testDeleteRecordsAfterCorruptRecords(groupProtocol: String): Unit = {
    ...
    val overrideConfig = new Properties
    overrideConfig.setProperty("auto.offset.reset", "earliest")
    val consumer = createConsumer(configOverrides = overrideConfig)
    consumer.subscribe(Seq(topic).asJava)
```

**How to spot**: Search for `getTestGroupProtocolParametersClassicGroupProtocolOnly` — each such annotation is a test that has not been validated against the consumer protocol. Any test that calls `seekToBeginning` on a pre-existing consumer group membership is a candidate for protocol-compatibility issues.

**Test**: The test now runs under both classic and consumer protocols. It produces records, truncates the log file to inject corruption, and then asserts that a fresh consumer poll throws `KafkaException` with the message `"Encountered corrupt message when fetching offset 0"`.
