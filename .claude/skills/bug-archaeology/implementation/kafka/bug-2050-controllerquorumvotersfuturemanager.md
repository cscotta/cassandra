**Commit**: a951b73fa5 · **Ticket**: KAFKA-17975
**Subsystem**: test-common/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.Node;
import java.net.InetSocketAddress;
import java.util.TreeMap;
    /**
     * This class manages a future which is completed with the proper value for
     * controller.quorum.voters once the randomly assigned ports for all the controllers are
     * known.
     */
    private static class ControllerQuorumVotersFutureManager implements AutoCloseable {
        private final int expectedControllers;
        private final CompletableFuture<Map<Integer, InetSocketAddress>> future = new CompletableFuture<>();
        private final Map<Integer, Integer> controllerPorts = new TreeMap<>();

// After
                    KafkaConfig config = createNodeConfig(node);
                        config,
                        CompletableFuture.completedFuture(QuorumConfig.parseVoterConnections(config.quorumConfig().voters())),

```

**How to spot**
When reviewing test-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17975 remove controllerquorumvotersfuturemanager (#17754).

**Test**
Test modified in KafkaClusterTestKit.java covering the corrected behaviour.
