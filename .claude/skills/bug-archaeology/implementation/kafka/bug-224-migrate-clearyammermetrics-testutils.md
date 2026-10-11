**Commit**: 6278e10100 · **Ticket**: KAFKA-20252
**Subsystem**: checkstyle/import-control-server-common.xml
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Mickael Maison <mickael.maison@gmail.com>

Co-authored-by: Kartikay Dubey <kartikay.dubey@meesho.com>

**Before/After**
```java
// Before
        kafka.utils.TestUtils.clearYammerMetrics();
        kafka.utils.TestUtils.clearYammerMetrics();
        kafka.utils.TestUtils.clearYammerMetrics();
import static org.apache.kafka.server.share.fetch.ShareFetchTestUtils.yammerMetricValue;

// After
            <!-- ServerTestUtils uses yammer metrics for test cleanup -->
            <allow pkg="org.apache.kafka.server.metrics" />
            <allow pkg="com.yammer.metrics.core" />
            <allow pkg="org.apache.kafka.server.util" />
import org.apache.kafka.server.util.ServerTestUtils;
        ServerTestUtils.clearYammerMetrics();
import org.apache.kafka.server.util.ServerTestUtils;
        ServerTestUtils.clearYammerMetrics();
import org.apache.kafka.server.util.ServerTestUtils;
        ServerTestUtils.clearYammerMetrics();
import static org.apache.kafka.server.util.ServerTestUtils.clearYammerMetrics;
import static org.apache.kafka.server.util.ServerTestUtils.yammerMetricValue;
```

**How to spot**
When reviewing checkstyle/import-control-server-common.xml code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20252: migrate clearyammermetrics to testutils (#21679).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
