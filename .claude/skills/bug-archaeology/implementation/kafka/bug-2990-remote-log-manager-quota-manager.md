**Commit**: bb7db87f98 · **Ticket**: KAFKA-15265
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Added the implementation of the quota manager that will be used to throttle copy and fetch requests from the remote storage. Reference KIP-956

Reviewers: Luke Chen <showuon@gmail.com>, Kamal Chandraprakash <kchandraprakash@uber.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
                            BrokerTopicStats brokerTopicStats) throws IOException {

// After
    <suppress checks="JavaNCSS"
              files="RemoteLogManagerTest.java"/>
import kafka.log.remote.quota.RLMQuotaManager;
import kafka.log.remote.quota.RLMQuotaManagerConfig;
import kafka.server.QuotaType;
import org.apache.kafka.common.metrics.Metrics;
    private final Metrics metrics;
    private final RLMQuotaManager rlmCopyQuotaManager;
    private final RLMQuotaManager rlmFetchQuotaManager;

     * @param metrics  Metrics instance
                            BrokerTopicStats brokerTopicStats,
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15265: add remote log manager quota manager (#15625).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
