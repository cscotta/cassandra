**Commit**: 2f086d188f · **Ticket**: KAFKA-18892
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Allow ClientQuotaCallback to implement Monitorable and register metrics. Reviewers: Mickael Maison <mickael.maison@gmail.com>, TaiJuWu 
<tjwu1217@gmail.com>, Jhen-Yung Hsu <jhenyunghsu@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.ClusterTestDefaults;
@ClusterTestDefaults(controllers = 3,
    types = {Type.KRAFT},
    serverProperties = {
        @ClusterConfigProperty(id = 3000, key = QuotaConfig.CLIENT_QUOTA_CALLBACK_CLASS_CONFIG, value = "org.apache.kafka.server.quota.CustomQuotaCallbackTest$CustomQuotaCallback"),
        @ClusterConfigProperty(id = 3001, key = QuotaConfig.CLIENT_QUOTA_CALLBACK_CLASS_CONFIG, value = "org.apache.kafka.server.quota.CustomQuotaCallbackTest$CustomQuotaCallback"),
        @ClusterConfigProperty(id = 3002, key = QuotaConfig.CLIENT_QUOTA_CALLBACK_CLASS_CONFIG, value = "org.apache.kafka.server.quota.CustomQuotaCallbackTest$CustomQuotaCallback"),
    }
)
public class CustomQuotaCallbackTest {
    private final ClusterInstance cluster;
    public CustomQuotaCallbackTest(ClusterInstance clusterInstance) {

// After
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.Gauge;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.Monitorable;
import org.apache.kafka.common.metrics.PluginMetrics;
import java.util.LinkedHashMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
public class CustomQuotaCallbackTest {
    private static int controllerId(Type type) {
        return type == Type.KRAFT ? 3000 : 0;
    @ClusterTest(
        controllers = 3,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18892: add kip-877 support for clientquotacallback (#19068).

**Test**
Test modified in CustomQuotaCallbackTest.java covering the corrected behaviour.
