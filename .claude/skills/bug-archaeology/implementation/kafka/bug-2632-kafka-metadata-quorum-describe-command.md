**Commit**: bc4df734b5 · **Ticket**: KAFKA-16521
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
describe --status now includes directory id and endpoint information for voter and observers. describe --replication now includes directory id. Reviewers: Colin P. McCabe <cmccabe@apache.org>, José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
  "latestVersionUnstable": true, // Version 2 is still under development.
            asList("NodeId", "LogEndOffset", "Lag", "LastFetchTimestamp", "LastCaughtUpTimestamp", "Status"),
            "\nCurrentVoters:          " + quorumInfo.voters().stream().map(QuorumInfo.ReplicaState::replicaId).map(Object::toString).collect(Collectors.joining(",", "[", "]")) +
            "\nCurrentObservers:       " + quorumInfo.observers().stream().map(QuorumInfo.ReplicaState::replicaId).map(Objects::toString).collect(Collectors.joining(",", "[", "]"))

// After
  "latestVersionUnstable": false,

      if (version >= 2) {
        val nodes = response.data.nodes().asScala
        assertEquals(cluster.controllerIds().asScala, nodes.map(_.nodeId()).toSet)
        val node = nodes.find(_.nodeId() == cluster.controllers().keySet().asScala.head)
        assertEquals(cluster.controllerListenerName().get().value(), node.get.listeners().asScala.head.name())
      }
import org.apache.kafka.clients.admin.RaftVoterEndpoint;
import org.apache.kafka.common.Uuid;
            asList("NodeId", "DirectoryId", "LogEndOffset", "Lag", "LastFetchTimestamp", "LastCaughtUpTimestamp", "Status"),
                info.replicaDirectoryId(),
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16521; kafka-metadata-quorum describe command changes for kip-853 (#16759).

**Test**
Test modified in DescribeQuorumRequestTest.scala covering the corrected behaviour.
