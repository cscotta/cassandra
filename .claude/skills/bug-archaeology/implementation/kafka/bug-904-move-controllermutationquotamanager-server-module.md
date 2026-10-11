**Commit**: e8ee7fc210 · **Ticket**: KAFKA-19315
**Subsystem**: checkstyle/import-control-server.xml
**Category**: Logic error in condition

**Root cause**
Migrate ControllerMutationQuotaManager to Java implementation and move
to server module, including ClientQuotaManager and associated files. Reviewers: TengYao Chi <kitingiao@gmail.com>, Ken Huang
<s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        Map<String, String> tags = new LinkedHashMap<>();
import scala.jdk.javaapi.CollectionConverters;
import scala.jdk.javaapi.OptionConverters;

        super(config, metrics, QuotaType.REQUEST, time, threadNamePrefix, OptionConverters.toScala(quotaCallbackPlugin));
     * Records that a user/clientId changed request processing time being throttled. If quota has been violated, return
     * throttle time in milliseconds. Throttle time calculation may be overridden by sub-classes.
    public MetricName clientQuotaMetricName(scala.collection.immutable.Map<String, String> quotaMetricTags) {
        return metrics.metricName("request-time", QuotaType.REQUEST.toString(), "Tracking request-time per user/client-id", CollectionConverters.asJava(quotaMetricTags));

// After
    <allow pkg="org.apache.kafka.network" />
import java.util.HashMap;
        var tags = new HashMap<String, String>();
import org.apache.kafka.server.quota.ClientQuotaManager;
import java.util.Map;
        super(config, metrics, QuotaType.REQUEST, time, threadNamePrefix, quotaCallbackPlugin);
     * Records that a user/clientId changed request processing time being throttled. If the quota has been violated, return
     * throttle time in milliseconds. Subclasses may override throttle time calculation.
    public MetricName clientQuotaMetricName(Map<String, String> quotaMetricTags) {
        return metrics.metricName("request-time", QuotaType.REQUEST.toString(), "Tracking request-time per user/client-id", quotaMetricTags);
```

**How to spot**
When reviewing checkstyle/import-control-server.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19315 move controllermutationquotamanager to server module (#19807).

**Test**
Test modified in CustomQuotaCallbackTest.java covering the corrected behaviour.
