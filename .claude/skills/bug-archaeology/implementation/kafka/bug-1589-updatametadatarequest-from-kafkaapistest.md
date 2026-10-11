**Commit**: 4772805cd5 · **Ticket**: KAFKA-18540
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.UpdateMetadataRequestData.{UpdateMetadataBroker, UpdateMetadataEndpoint, UpdateMetadataPartitionState}
    val broker = new UpdateMetadataBroker()
      .setId(0)
      .setRack("rack")
      .setEndpoints(Seq(
        new UpdateMetadataEndpoint()
          .setHost("broker0")
          .setPort(9092)
          .setSecurityProtocol(SecurityProtocol.PLAINTEXT.id)
          .setListener(plaintextListener.value)
      ).asJava)
    val topicIds = new util.HashMap[String, Uuid]()

// After
import org.apache.kafka.common.metadata.{TopicRecord, PartitionRecord, RegisterBrokerRecord}
import org.apache.kafka.common.metadata.RegisterBrokerRecord.{BrokerEndpoint, BrokerEndpointCollection}
import org.apache.kafka.common.protocol.ApiMessage
    val endpoints = new BrokerEndpointCollection()
    endpoints.add(
      new BrokerEndpoint()
        .setHost("broker0")
        .setPort(9092)
        .setSecurityProtocol(SecurityProtocol.PLAINTEXT.id)
        .setName(plaintextListener.value)
    )
    MetadataCacheTest.updateCache(metadataCache,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18540: remove `updatametadatarequest` from `kafkaapistest` (#18591).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
