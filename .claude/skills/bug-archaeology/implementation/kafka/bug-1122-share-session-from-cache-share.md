**Commit**: 800612e4a7 · **Ticket**: KAFKA-19015
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Up till now, the share sessions in the broker were only attempted to
evict when the share session cache was full and a new session was trying
to get registered. With the changes in this PR, whenever a share
consumer gets disconnected from the broker, the corresponding share
session would be evicted from the cache. Note - `connectAndReceiveWithoutClosingSocket` has been introduced in
`GroupCoordinatorBaseRequestTest`. This method creates a socket
connection, sends the request, receives a response but does not close
the connection.

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.errors.GroupMaxSizeReachedException;
    @ClusterTest(
        brokers = 1,
        serverProperties = {
            @ClusterConfigProperty(key = "auto.create.topics.enable", value = "false"),
            @ClusterConfigProperty(key = "group.coordinator.rebalance.protocols", value = "classic,consumer,share"),
            @ClusterConfigProperty(key = "group.share.enable", value = "true"),
            @ClusterConfigProperty(key = "group.share.partition.max.record.locks", value = "10000"),
            @ClusterConfigProperty(key = "group.share.record.lock.duration.ms", value = "15000"),
            @ClusterConfigProperty(key = "offsets.topic.replication.factor", value = "1"),
            @ClusterConfigProperty(key = "share.coordinator.state.topic.min.isr", value = "1"),
            @ClusterConfigProperty(key = "share.coordinator.state.topic.num.partitions", value = "3"),
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19015: remove share session from cache on share consumer connection drop (#19329).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
