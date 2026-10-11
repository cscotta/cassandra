**Commit**: f995edccad · **Ticket**: KAFKA-16551
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import kafka.test.annotation.Type;
import org.apache.kafka.common.errors.UnsupportedEndpointTypeException;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
    @ClusterTest(types = {Type.KRAFT, Type.CO_KRAFT})
    public void testClusterIdWithBootstrapController(ClusterInstance clusterInstance) {
        String output = ToolsTestUtils.captureStandardOut(() ->
                assertDoesNotThrow(() -> ClusterTool.execute("cluster-id", "--bootstrap-controller", clusterInstance.bootstrapControllers())));
        assertTrue(output.contains("Cluster ID: " + clusterInstance.clusterId()));
    }

```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16551 add integration test with bootstrap controller for clustertool (#16439).

**Test**
Test modified in ClusterToolTest.java covering the corrected behaviour.
