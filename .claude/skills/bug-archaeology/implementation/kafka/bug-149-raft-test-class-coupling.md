**Commit**: 6f5be29374 · **Ticket**: KAFKA-18332
**Subsystem**: raft/src/test/java/org/apache/kafka/raft/KafkaRaftClientTest.java
**Category**: Logic error in condition

**Root cause**
`KafkaRaftClientTest` had accumulated so many imported classes that it violated the Checkstyle `ClassDataAbstractionCoupling` limit, causing build failures. The root cause was that the file mixed cluster-authorization test scenarios with the main raft-client tests, inflating its import count beyond the threshold. This is a pure test-organisation issue, not a functional bug.

**Before/After**
Cluster-authorization response tests (for `FETCH`, `BeginQuorumEpoch`, `EndQuorumEpoch`, `Vote`) were extracted into a new file `KafkaRaftClientClusterAuthTest.java`, reducing the import count in the original file.

**How to spot**
Monitor Checkstyle `ClassDataAbstractionCoupling` violations in large test files. When a single test class covers many unrelated scenarios, consider splitting by concern to stay within the coupling threshold and improve navigability.

**Test**
This commit is itself a test restructuring. The new `KafkaRaftClientClusterAuthTest` contains parameterised tests for `CLUSTER_AUTHORIZATION_FAILED` responses in Fetch, BeginQuorumEpoch, EndQuorumEpoch, and Vote RPCs, with and without KIP-853 RPC enabled.
