**Commit**: 92d8cb562a · **Ticket**: KAFKA-19078
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add the `controller.quorum.auto.join.enable` configuration. When enabled
with KIP-853 supported, follower controllers who are observers (their
replica id + directory id are not in the voter set) will:

- Automatically remove voter set entries which match their replica id
but not directory id by sending the `RemoveVoterRPC` to the leader. - Automatically add themselves as a voter when their replica id is not
present in the voter set by sending the `AddVoterRPC` to the leader. Reviewers: José Armando García Sancio
 [jsancio@apache.org](mailto:jsancio@apache.org),   Chia-Ping Tsai
 [chia7712@gmail.com](mailto:chia7712@gmail.com)

**Before/After**
```java
// Before
// (no removals)

// After
      // controller.quorum.auto.join.enable must be false for KRaft broker-only
      require(!quorumConfig.autoJoin,
        s"${QuorumConfig.QUORUM_AUTO_JOIN_ENABLE_CONFIG} is only supported when ${KRaftConfigs.PROCESS_ROLES_CONFIG} contains the 'controller' role.")
import org.apache.kafka.common.test.api.TestKitDefaults;
import org.apache.kafka.raft.QuorumConfig;
import java.util.HashMap;

    @Test
    public void testControllersAutoJoinStandaloneVoter() throws Exception {
        final var nodes = new TestKitNodes.Builder().
            setNumBrokerNodes(1).
            setNumControllerNodes(3).
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19078 automatic controller addition to cluster metadata partition (#19589).

**Test**
Test modified in ReconfigurableQuorumIntegrationTest.java covering the corrected behaviour.
