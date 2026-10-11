**Commit**: 9f955973fe · **Ticket**: KAFKA-18399
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        case ApiKeys.ALLOCATE_PRODUCER_IDS => handleAllocateProducerIdsRequest(request)
  def handleAlterClientQuotasRequest(request: RequestChannel.Request): Unit = {
    val zkSupport = metadataSupport.requireZkOrThrow(KafkaApis.shouldAlwaysForward(request))
    val alterClientQuotasRequest = request.body[AlterClientQuotasRequest]

    if (authHelper.authorize(request.context, ALTER_CONFIGS, CLUSTER, CLUSTER_NAME)) {
      val result = zkSupport.adminManager.alterClientQuotas(alterClientQuotasRequest.entries.asScala,
        alterClientQuotasRequest.validateOnly)

      val entriesData = result.iterator.map { case (quotaEntity, apiError) =>
        val entityData = quotaEntity.entries.asScala.iterator.map { case (key, value) =>
          new AlterClientQuotasResponseData.EntityData()

// After
import org.apache.kafka.common.internals.Topic
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18399 remove zookeeper from kafkaapis (9/n): alter_client_quotas and allocate_producer_ids (#18465).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
