**Commit**: 66147d5de7 · **Ticket**: KAFKA-19057
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR removes the unstable API flag for the KIP-932 RPCs. The 4 RPCs which were exposed for the early access release in AK 4.0 are
stabilised at v1. This is because the RPCs have evolved over time and AK
4.0 clients are not compatible with AK 4.1 brokers. By stabilising at
v1, the API version checks prevent incompatible communication and
server-side exceptions when trying to parse the requests from the older
clients.

**Before/After**
```java
// Before
        @ClusterConfigProperty(key = "transaction.state.log.replication.factor", value = "1"),
        @ClusterConfigProperty(key = "unstable.api.versions.enable", value = "true")
            @ClusterConfigProperty(key = "share.coordinator.state.topic.replication.factor", value = "3"),
            @ClusterConfigProperty(key = "unstable.api.versions.enable", value = "true")
            @ClusterConfigProperty(key = "share.coordinator.state.topic.replication.factor", value = "3"),
            @ClusterConfigProperty(key = "unstable.api.versions.enable", value = "true")
                .setTopics(topics),
            true
        return new ShareGroupDescribeRequest.Builder(data, true);
        return new DescribeShareGroupOffsetsRequest.Builder(data, true);

// After
        @ClusterConfigProperty(key = "transaction.state.log.replication.factor", value = "1")
            @ClusterConfigProperty(key = "share.coordinator.state.topic.replication.factor", value = "3")
            @ClusterConfigProperty(key = "share.coordinator.state.topic.replication.factor", value = "3")
                .setTopics(topics)
        return new ShareGroupDescribeRequest.Builder(data);
        return new DescribeShareGroupOffsetsRequest.Builder(data);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19057: stabilize kip-932 rpcs for ak 4.1 (#19378).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
