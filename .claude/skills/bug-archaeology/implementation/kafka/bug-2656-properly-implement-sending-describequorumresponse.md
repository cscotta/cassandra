**Commit**: 2cf87bff9b · **Ticket**: KAFKA-16953
**Subsystem**: raft/src
**Category**: Wrong serialization / deserialization

**Root cause**
This change allows the KRaft leader to send the DescribeQuorumResponse version based on the schema version used by the client. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
            leaderState.describeQuorum(currentTimeMs),
            leaderState.nodes(currentTimeMs)
import org.apache.kafka.common.message.DescribeQuorumResponseData;
import java.util.Collection;
                new ReplicaState(voterNode.voterKey(), hasAcknowledgedLeader)
            observerStates.putIfAbsent(replicaKey, new ReplicaState(replicaKey, false));
    public DescribeQuorumResponseData.PartitionData describeQuorum(long currentTimeMs) {
        clearInactiveObservers(currentTimeMs);

        return new DescribeQuorumResponseData.PartitionData()
            .setErrorCode(Errors.NONE.code())
            .setLeaderId(localReplicaKey.id())

// After
import org.apache.kafka.common.message.DescribeQuorumResponseData;
        return listeners;
    }
    public DescribeQuorumResponseData.ListenerCollection toDescribeQuorumResponseListeners() {
        DescribeQuorumResponseData.ListenerCollection listeners =
            new DescribeQuorumResponseData.ListenerCollection(endpoints.size());
        for (Map.Entry<ListenerName, InetSocketAddress> entry : endpoints.entrySet()) {
            listeners.add(
                new DescribeQuorumResponseData.Listener()
                    .setName(entry.getKey().value())
                    .setHost(entry.getValue().getHostString())
                    .setPort(entry.getValue().getPort())
```

**How to spot**
When reviewing raft/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16953; properly implement the sending of describequorumresponse (#16637).

**Test**
Test modified in KafkaRaftClientTest.java covering the corrected behaviour.
