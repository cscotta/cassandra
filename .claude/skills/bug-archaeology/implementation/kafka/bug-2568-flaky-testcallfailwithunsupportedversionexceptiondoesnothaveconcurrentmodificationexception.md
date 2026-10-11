**Commit**: dc636ea1ff · **Ticket**: KAFKA-17309
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        try (AdminClientUnitTestEnv env = new AdminClientUnitTestEnv(mockCluster(1, 0))) {
            env.kafkaClient().setNodeApiVersions(NodeApiVersions.create());
                    new AdminClientConfig(Collections.emptyMap()), metadataManager, env.kafkaClient(), env.time())) {
                admin.describeCluster(new DescribeClusterOptions().timeoutMs(1000));
                TestUtils.waitForCondition(() -> env.kafkaClient().inFlightRequestCount() != 0,

// After
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
        Cluster cluster = mockCluster(1, 0);
        try (MockClient mockClient = new MockClient(Time.SYSTEM, new MockClient.MockMetadataUpdater() {
            @Override
            public List<Node> fetchNodes() {
                return cluster.nodes();
            }

            @Override
            public boolean isUpdateNeeded() {
                return false;
            }
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17309 fix flaky testcallfailwithunsupportedversionexceptiondoesnothaveconcurrentmodificationexception (#16854).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
