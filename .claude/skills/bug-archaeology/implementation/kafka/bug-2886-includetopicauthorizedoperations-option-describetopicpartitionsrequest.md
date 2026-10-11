**Commit**: 9368ef81b5 · **Ticket**: KAFKA-16865
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>, Calvin Liu <caliu@confluent.io>, Andrew Schofield <andrew_schofield@live.com>, Apoorv Mittal <amittal@confluent.io>

**Before/After**
```java
// Before
                    TopicDescription currentTopicDescription = getTopicDescriptionFromDescribeTopicsResponseTopic(topic, nodes);
        Map<Integer, Node> nodes
        return new TopicDescription(topic.name(), topic.isInternal(), partitions, validAclOperations(topic.topicAuthorizedOperations()), topic.topicId());

// After
                    TopicDescription currentTopicDescription = getTopicDescriptionFromDescribeTopicsResponseTopic(topic, nodes, options.includeAuthorizedOperations());
        Map<Integer, Node> nodes,
        boolean includeAuthorizedOperations
        Set<AclOperation> authorisedOperations = includeAuthorizedOperations ? validAclOperations(topic.topicAuthorizedOperations()) : null;
        return new TopicDescription(topic.name(), topic.isInternal(), partitions, authorisedOperations, topic.topicId());
                assertNull(topicDescription.authorizedOperations());
    @Test
    public void testDescribeTopicPartitionsApiWithAuthorizedOps() throws ExecutionException, InterruptedException {
        try (AdminClientUnitTestEnv env = mockClientEnv()) {
            env.kafkaClient().setNodeApiVersions(NodeApiVersions.create());
            String topicName0 = "test-0";
            Uuid topicId =  Uuid.randomUuid();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16865: add includetopicauthorizedoperations option for describetopicpartitionsrequest (#16136).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
