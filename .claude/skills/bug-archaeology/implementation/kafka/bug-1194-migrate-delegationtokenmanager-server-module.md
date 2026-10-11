**Commit**: 8827ce4701 · **Ticket**: KAFKA-19113
**Subsystem**: checkstyle/import-control-server.xml
**Category**: Logic error in condition

**Root cause**
1. Migrate DelegationTokenManager to server module. 2. Rewrite DelegationTokenManager in Java.

**Before/After**
```java
// Before
import kafka.server.DelegationTokenManager;
import org.apache.kafka.server.config.ConfigType
import org.apache.kafka.server.{AssignmentsManager, BrokerFeatures, ClientMetricsManager, DelayedActionQueue, ProcessRole}
      tokenManager = new DelegationTokenManager(config, tokenCache, time)
import org.apache.kafka.server.ProcessRole
              .setExpiryTimestampMs(DelegationTokenManager.ErrorTimestamp)))
              .setExpiryTimestampMs(DelegationTokenManager.ErrorTimestamp)))

// After
    <allow pkg="javax.crypto" />
import org.apache.kafka.server.DelegationTokenManager;
import org.apache.kafka.server.config.{ConfigType, DelegationTokenManagerConfigs}
import org.apache.kafka.server.{AssignmentsManager, BrokerFeatures, ClientMetricsManager, DelayedActionQueue, DelegationTokenManager, ProcessRole}
      tokenManager = new DelegationTokenManager(new DelegationTokenManagerConfigs(config), tokenCache)
import org.apache.kafka.server.{DelegationTokenManager, ProcessRole}
              .setExpiryTimestampMs(DelegationTokenManager.ERROR_TIMESTAMP)))
              .setExpiryTimestampMs(DelegationTokenManager.ERROR_TIMESTAMP)))
```

**How to spot**
When reviewing checkstyle/import-control-server.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19113: migrate delegationtokenmanager to server module (#19424).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
