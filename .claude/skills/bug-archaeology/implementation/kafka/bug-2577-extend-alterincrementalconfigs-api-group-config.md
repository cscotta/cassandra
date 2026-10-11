**Commit**: bbdf79e1b4 · **Ticket**: KAFKA-14511
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This patch add resources to store and handle consumer group's config. Reviewers: Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
        CLIENT_METRICS((byte) 16), BROKER_LOGGER((byte) 8), BROKER((byte) 4), TOPIC((byte) 2), UNKNOWN((byte) 0);
        CLIENT_METRICS_CONFIG((byte) 7, org.apache.kafka.clients.admin.ConfigEntry.ConfigSource.DYNAMIC_CLIENT_METRICS_CONFIG);

// After
        DYNAMIC_GROUP_CONFIG,           // dynamic group config that is configured for a specific group
        GROUP((byte) 32),
        CLIENT_METRICS((byte) 16),
        BROKER_LOGGER((byte) 8),
        BROKER((byte) 4),
        TOPIC((byte) 2),
        UNKNOWN((byte) 0);
import org.apache.kafka.clients.admin.ConfigEntry;
        CLIENT_METRICS_CONFIG((byte) 7, org.apache.kafka.clients.admin.ConfigEntry.ConfigSource.DYNAMIC_CLIENT_METRICS_CONFIG),
        GROUP_CONFIG((byte) 8, org.apache.kafka.clients.admin.ConfigEntry.ConfigSource.DYNAMIC_GROUP_CONFIG);
import org.apache.kafka.common.errors.InvalidConfigurationException;
            responseData.responses().add(new AlterConfigsResourceResponse()
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14511; extend alterincrementalconfigs api to support group config (#15067).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
