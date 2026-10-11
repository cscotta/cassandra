**Commit**: 4e69bc09e6 · **Ticket**: KAFKA-17194
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private final ClusterInstance cluster;
    public MetadataQuorumCommandTest(ClusterInstance cluster) {
        this.cluster = cluster;
    }

    public void testDescribeQuorumReplicationSuccessful() throws InterruptedException {

    public void testDescribeQuorumStatusSuccessful() throws InterruptedException {
    public void testOnlyOneBrokerAndOneController() {
    @ClusterTest(types = {Type.CO_KRAFT})
        assertEquals(1, MetadataQuorumCommand.mainNoExit("--bootstrap-server", cluster.bootstrapServers(),
    public void testDescribeQuorumInZkMode() {

// After
import org.junit.jupiter.api.Test;
    public void testDescribeQuorumReplicationSuccessful(ClusterInstance cluster) throws InterruptedException {
    public void testDescribeQuorumStatusSuccessful(ClusterInstance cluster) throws InterruptedException {
    public void testOnlyOneBrokerAndOneController(ClusterInstance cluster) {
    @Test
        assertEquals(1, MetadataQuorumCommand.mainNoExit("--bootstrap-server", "localhost:9092",
    public void testDescribeQuorumInZkMode(ClusterInstance cluster) {
    public void testHumanReadableOutput(ClusterInstance cluster) {
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17194 don't create cluster for metadataquorumcommandtest#testcommandconfig (#16682).

**Test**
Test modified in MetadataQuorumCommandTest.java covering the corrected behaviour.
