**Commit**: 3a0c83e490 · **Ticket**: KAFKA-16551
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import kafka.test.ClusterInstance;
import kafka.test.annotation.ClusterTest;
import kafka.test.junit.ClusterTestExtensions;

import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
@ExtendWith(value = ClusterTestExtensions.class)
    @ClusterTest
    public void testClusterId(ClusterInstance clusterInstance) {
        String output = ToolsTestUtils.captureStandardOut(() ->
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16551 add integration test for clustertool (#16408).

**Test**
Test modified in ClusterToolTest.java covering the corrected behaviour.
