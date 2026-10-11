**Commit**: 431cffc93f · **Ticket**: KAFKA-19135
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR is a migration of the initial IQ support for KIP-1071 from the
feature branch to trunk. It includes a parameterized integration test
that expects the same results whether using either the classic or new
streams group protocol. Note that this PR will deliver IQ information in each heartbeat
response. A follow-up PR will change that to be only sending IQ
information when assignments change.

**Before/After**
```java
// Before
    private static Map<StreamsRebalanceData.HostInfo, List<TopicPartition>> convertHostInfoMap(final StreamsGroupHeartbeatResponseData data) {
        Map<StreamsRebalanceData.HostInfo, List<TopicPartition>> partitionsByHost = new HashMap<>();
            List<TopicPartition> topicPartitions = endpoint.partitions().stream()
                .flatMap(partition ->
                    partition.partitions().stream().map(partitionId -> new TopicPartition(partition.topic(), partitionId)))
                .collect(Collectors.toList());
            partitionsByHost.put(new StreamsRebalanceData.HostInfo(userEndpoint.host(), userEndpoint.port()), topicPartitions);
    private final AtomicReference<Map<HostInfo, List<TopicPartition>>> partitionsByHost = new AtomicReference<>(Collections.emptyMap());
    public void setPartitionsByHost(final

// After
    private static Map<StreamsRebalanceData.HostInfo, StreamsRebalanceData.EndpointPartitions> convertHostInfoMap(
            final StreamsGroupHeartbeatResponseData data) {
        Map<StreamsRebalanceData.HostInfo, StreamsRebalanceData.EndpointPartitions> partitionsByHost = new HashMap<>();
            List<TopicPartition> activeTopicPartitions = getTopicPartitionList(endpoint.activePartitions());
            List<TopicPartition> standbyTopicPartitions = getTopicPartitionList(endpoint.standbyPartitions());
            StreamsRebalanceData.EndpointPartitions endpointPartitions = new StreamsRebalanceData.EndpointPartitions(activeTopicPartitions, standbyTopicPartitions);
            partitionsByHost.put(new StreamsRebalanceData.HostInfo(userEndpoint.host(), userEndpoint.port()), endpointPartitions);

    static List<TopicPartition> getTopicPartitionList(List<StreamsGroupHeartbeatResponseData.TopicPartition> topicPartitions) {
        return topicPartitions.stream()
                .flatMap(partition ->
                        partition.partitions().stream().map(partitionId -> new TopicPartition(partition.topic(), partitionId)))
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19135 migrate initial iq support for kip-1071 from feature branch to trunk (#19588).

**Test**
Test modified in StreamsGroupHeartbeatRequestManagerTest.java covering the corrected behaviour.
