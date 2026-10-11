**Commit**: ceee1a732e · **Ticket**: KAFKA-18346
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Due to an issue with handling folders in Kafka version 3.3.2 (see https://github.com/apache/kafka/pull/13130), this end-to-end test requires using a single folder for upgrade/downgrade scenarios involving 3.3.2. Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        :param list[list] server_prop_overrides: overrides for kafka.properties file
        prop_file = self.render_configs(configs)
from kafkatest.services.kafka import KafkaService
                                                       'configs': {"min.insync.replicas": 2}}})

// After
        :param list[list] server_prop_overrides: overrides for kafka.properties file, if the second value is None or "", it will be filtered
        filtered_configs = {k: v for k, v in configs.items() if v not in [None, ""]}
        prop_file = self.render_configs(filtered_configs)
from kafkatest.services.kafka import config_property, KafkaService

        # Due to compatability issue with version 3.3, we need to use a single folder. Using multiple folders
        # will cause broker to throw InconsistentBrokerMetadataException during startup.
        # see https://github.com/apache/kafka/pull/13130
        server_prop_overrides = None
        if starting_kafka_version == str(LATEST_3_3):
            server_prop_overrides = [[config_property.LOG_DIRS, "/mnt/kafka/kafka-metadata-logs"], [config_property.METADATA_LOG_DIR, ""]]

```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18346 fix e2e testkraftupgrade for v3.3.2 (#18386).

**Test**
Test modified in kafka.py covering the corrected behaviour.
