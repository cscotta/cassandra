**Commit**: a5bfc2190c · **Ticket**: KAFKA-16791
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
public class ClusterTestExtensions implements TestTemplateInvocationContextProvider {

// After
import org.apache.kafka.test.TestUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
public class ClusterTestExtensions implements TestTemplateInvocationContextProvider, BeforeEachCallback, AfterEachCallback {
    private static final String METRICS_METER_TICK_THREAD_PREFIX = "metrics-meter-tick-thread";
    private static final String SCALA_THREAD_PREFIX = "scala-";
    private static final String FORK_JOIN_POOL_THREAD_PREFIX = "ForkJoinPool";
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16791 add thread detection to clustertestextensions (#16499).

**Test**
Test modified in ClusterTestExtensions.java covering the corrected behaviour.
