**Commit**: 3a0efa2845 · **Ticket**: KAFKA-14510
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch extends the DescribeConfigs API to support group configs. Reviewers: Andrew Schofield <aschofield@confluent.io>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before


import org.apache.kafka.common.resource.ResourceType.{CLUSTER, TOPIC}

// After
    @Test
    public void testDescribeConsumerGroupConfigs() throws Exception {
        ConfigResource resource1 = new ConfigResource(ConfigResource.Type.GROUP, "group1");
        ConfigResource resource2 = new ConfigResource(ConfigResource.Type.GROUP, "group2");
        try (AdminClientUnitTestEnv env = mockClientEnv()) {
            env.kafkaClient().setNodeApiVersions(NodeApiVersions.create());
            env.kafkaClient().prepareResponse(new DescribeConfigsResponse(
                new DescribeConfigsResponseData().setResults(asList(
                    new DescribeConfigsResponseData.DescribeConfigsResult()
                        .setResourceName(resource1.name())
                        .setResourceType(resource1.type().id())
                        .setErrorCode(Errors.NONE.code())
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14510; extend describeconfigs api to support group configs (#16859).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
