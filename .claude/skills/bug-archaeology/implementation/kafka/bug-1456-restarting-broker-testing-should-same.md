**Commit**: 0621c0b4de · **Ticket**: KAFKA-18275
**Subsystem**: connect/runtime
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.network.SocketServerConfigs;
import java.io.IOException;
import java.net.ServerSocket;
        useFixedBrokerPort();

        useFixedBrokerPort();

    private void useFixedBrokerPort() throws IOException {
        // Find a free port and use it in the Kafka broker's listeners config. We can't use port 0 in the listeners
        // config to get a random free port because in this test we want to stop the Kafka broker and then bring it
        // back up and listening on the same port in order to verify that the Connect cluster can re-connect to Kafka
        // and continue functioning normally. If we were to use port 0 here, the Kafka broker would most likely listen

// After
        private Optional<File> maybeSetupJaasFile() throws Exception {
                File file = JaasUtils.writeJaasContextsToFile(Set.of(
                                JaasUtils.KAFKA_PLAIN_ADMIN,
                                JaasUtils.KAFKA_PLAIN_ADMIN_PASSWORD,
                            )
                JaasUtils.refreshJavaLoginConfigParam(file);
                return Optional.of(file);
            return Optional.empty();
        }
        public KafkaClusterTestKit build() throws Exception {
            Map<Integer, ControllerServer> controllers = new HashMap<>();
            Map<Integer, BrokerServer> brokers = new HashMap<>();
```

**How to spot**
When reviewing connect/runtime code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18275 restarting broker in testing should use the same port (#18381).

**Test**
Test modified in ConnectWorkerIntegrationTest.java covering the corrected behaviour.
