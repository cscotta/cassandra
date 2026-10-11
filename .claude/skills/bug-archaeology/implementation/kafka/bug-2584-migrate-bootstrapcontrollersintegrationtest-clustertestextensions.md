**Commit**: 49fc14f611 · **Ticket**: KAFKA-17273
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.ControllerServer;
import kafka.testkit.KafkaClusterTestKit;
import kafka.testkit.TestKitNodes;
import org.apache.kafka.controller.QuorumController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Arrays;
import java.util.Properties;

// After
    <subpackage name="server">
      <allow pkg="kafka.test" />
    </subpackage>
import kafka.test.ClusterInstance;
import kafka.test.annotation.ClusterTest;
import kafka.test.annotation.ClusterTestDefaults;
import kafka.test.annotation.Type;
import kafka.test.junit.ClusterTestExtensions;
import org.junit.jupiter.api.extension.ExtendWith;
@ExtendWith(ClusterTestExtensions.class)
@ClusterTestDefaults(types = {Type.KRAFT})
    private Map<String, Object> adminConfig(ClusterInstance clusterInstance, boolean usingBootstrapControllers) {
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17273 migrate bootstrapcontrollersintegrationtest to use clustertestextensions (#16822).

**Test**
Test modified in BootstrapControllersIntegrationTest.java covering the corrected behaviour.
