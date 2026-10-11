**Commit**: cfe85c2a81 · **Ticket**: KAFKA-19802
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Admin client changes for KIP-1226 which adds lag information for share
groups. Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
        SimpleAdminApiFuture<CoordinatorKey, Map<TopicPartition, OffsetAndMetadata>> future = ListShareGroupOffsetsHandler.newFuture(groupSpecs.keySet());
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
    private final Map<String, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> futures;
    ListShareGroupOffsetsResult(final Map<CoordinatorKey, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> futures) {
     * @return Future which yields all {@code Map<String, Map<TopicPartition, OffsetAndMetadata>>} objects, if requests for all the groups succeed.
    public KafkaFuture<Map<String, Map<TopicPartition, OffsetAndMetadata>>> all() {
                Map<String, Map<TopicPartition, OffsetAndMetadata>> offsets = new HashMap<>(futures.size());
     * have a committed offset for a specific partition, the corresponding value in the returned map will be null.
    public KafkaFuture<Map<TopicPartition, OffsetAndMetadata>> partitionsToOffsetAndMetadata(String groupId) {

// After
        SimpleAdminApiFuture<CoordinatorKey, Map<TopicPartition, SharePartitionOffsetInfo>> future = ListShareGroupOffsetsHandler.newFuture(groupSpecs.keySet());
    private final Map<String, KafkaFuture<Map<TopicPartition, SharePartitionOffsetInfo>>> futures;
    ListShareGroupOffsetsResult(final Map<CoordinatorKey, KafkaFuture<Map<TopicPartition, SharePartitionOffsetInfo>>> futures) {
     * @return Future which yields all {@code Map<String, Map<TopicPartition, SharePartitionOffsetInfo>>} objects, if requests for all the groups succeed.
    public KafkaFuture<Map<String, Map<TopicPartition, SharePartitionOffsetInfo>>> all() {
                Map<String, Map<TopicPartition, SharePartitionOffsetInfo>> offsets = new HashMap<>(futures.size());
     * have offset information for a specific partition, the corresponding value in the returned map will be null.
    public KafkaFuture<Map<TopicPartition, SharePartitionOffsetInfo>> partitionsToOffsetInfo(String groupId) {
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19802: admin client changes for kip-1226 (#20771).

**Test**
Test modified in AdminClientTestUtils.java covering the corrected behaviour.
