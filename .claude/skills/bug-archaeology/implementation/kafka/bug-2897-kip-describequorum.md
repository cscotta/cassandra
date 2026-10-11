**Commit**: aecaf44475 · **Ticket**: KAFKA-16520
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Add support for KIP-953 KRaft Quorum reconfiguration in the DescribeQuorum request and response. Also add support to AdminClient.describeQuorum, so that users will be able to find the current set of
quorum nodes, as well as their directories, via these RPCs. Reviewers: Luke Chen <showuon@gmail.com>, Colin P. McCabe <cmccabe@apache.org>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            private QuorumInfo createQuorumResult(final DescribeQuorumResponseData.PartitionData partition) {
                    observers
                    throw Errors.forCode(quorumResponse.data().errorCode()).exception();
                    throw Errors.forCode(partition.errorCode()).exception();
                future.complete(createQuorumResult(partition));
        List<ReplicaState> observers

// After
                        replica.replicaDirectoryId() == null ? Uuid.ZERO_UUID : replica.replicaDirectoryId(),
            private QuorumInfo createQuorumResult(final DescribeQuorumResponseData.PartitionData partition, DescribeQuorumResponseData.NodeCollection nodeCollection) {
                Map<Integer, QuorumInfo.Node> nodes = nodeCollection.stream().map(n -> {
                    List<RaftVoterEndpoint> endpoints = n.listeners().stream()
                        .map(l -> new RaftVoterEndpoint(l.name(), l.host(), l.port()))
                        .collect(Collectors.toList());

                    return new QuorumInfo.Node(n.nodeId(), endpoints);
                }).collect(Collectors.toMap(QuorumInfo.Node::nodeId, Function.identity()));

                    observers,
                    nodes
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16520: support kip-853 in describequorum (#16106).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
