**Commit**: 839431e591 · **Ticket**: KAFKA-17468
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.log.remote.quota.RLMQuotaManager;
import kafka.log.remote.quota.RLMQuotaManagerConfig;
import kafka.log.remote.quota.RLMQuotaMetrics;
import static kafka.log.remote.quota.RLMQuotaManagerConfig.INACTIVE_SENSOR_EXPIRATION_TIME_SECONDS;
import kafka.log.remote.quota.RLMQuotaManager;

import kafka.log.remote.quota.RLMQuotaManager;
import kafka.log.remote.quota.RLMQuotaManagerConfig;

// After
    <allow pkg="org.apache.kafka.server.log.remote.quota" />
                <subpackage name="quota">
                    <allow pkg="org.apache.kafka.server.quota" />
                </subpackage>
import org.apache.kafka.server.log.remote.quota.RLMQuotaManager;
import org.apache.kafka.server.log.remote.quota.RLMQuotaManagerConfig;
import org.apache.kafka.server.log.remote.quota.RLMQuotaMetrics;
import static org.apache.kafka.server.log.remote.quota.RLMQuotaManagerConfig.INACTIVE_SENSOR_EXPIRATION_TIME_SECONDS;
import org.apache.kafka.server.log.remote.quota.RLMQuotaManager;
import org.apache.kafka.server.log.remote.quota.RLMQuotaManager;
import org.apache.kafka.server.log.remote.quota.RLMQuotaManagerConfig;
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17468 move kafka/log/remote/quota classes to storage module (#17074).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
