**Commit**: e12db663f0 · **Ticket**: KAFKA-18514
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
The PR removes dependency of server module on share-coordinator, rather it should be other way. Moved the ShareCoordinatorConfig class from server to share-coordinator. Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    implementation project(':server')
import org.apache.kafka.server.config.{AbstractKafkaConfig, DelegationTokenManagerConfigs, KRaftConfigs, QuotaConfig, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ShareCoordinatorConfig, ZkConfigs}
package org.apache.kafka.server.config;
import org.apache.kafka.server.config.ShareCoordinatorConfig;

// After
    implementation project(':share-coordinator')
import org.apache.kafka.coordinator.share.ShareCoordinatorConfig
import org.apache.kafka.server.config.{AbstractKafkaConfig, DelegationTokenManagerConfigs, KRaftConfigs, QuotaConfig, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ZkConfigs}
import org.apache.kafka.coordinator.share.ShareCoordinatorConfig;
package org.apache.kafka.coordinator.share;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18514 remove server dependency on share coordinator (#18536).

**Test**
Test modified in ShareCoordinatorShardTest.java covering the corrected behaviour.
