**Commit**: e0c77140b2 · **Ticket**: KAFKA-17039
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                new LeastLoadedNodeProvider()) {
  @Test
  def testUnregisterBroker(): Unit = {
      val admin = Admin.create(cluster.clientProperties())
import org.apache.kafka.common.errors.UnsupportedEndpointTypeException;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.assertNotNull;
        ExecutionException exception =
                assertThrows(ExecutionException.class,
                        () -> ClusterTool.execute("unregister", "--bootstrap-controller", clusterInstance.bootstrapControllers(), "--id", String.valueOf(brokerId)));
        assertNotNull(exception.getCause());
        assertEquals(UnsupportedEndpointTypeException.class, exception.getCause().getClass());

// After
                new LeastLoadedBrokerOrActiveKController()) {
  @ParameterizedTest
  @ValueSource(booleans = Array(true, false))
  def testUnregisterBroker(usingBootstrapController: Boolean): Unit = {
      val admin = createAdminClient(cluster, bootstrapController = usingBootstrapController);
        assertDoesNotThrow(() -> ClusterTool.execute("unregister", "--bootstrap-controller", clusterInstance.bootstrapControllers(), "--id", String.valueOf(brokerId)));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17039 kip-919 supports for unregisterbroker (#19063).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
