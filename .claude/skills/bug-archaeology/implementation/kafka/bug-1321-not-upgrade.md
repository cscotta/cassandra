**Commit**: b8886b84b6 · **Ticket**: KAFKA-18941
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
The upgrade test in question is not supported for AK 3.3.2 due to a
[known issue](https://issues.apache.org/jira/browse/KAFKA-18442). Previous attempt at solving this left the `metadata.log.dir` empty which
leads to the following crash log:

```
ERROR Exiting Kafka due to fatal exception (kafka.Kafka$)
org.apache.kafka.common.KafkaException: No `meta.properties` found in  (have you run `kafka-storage.sh` to format the directory?)
	at kafka.server.BrokerMetadataCheckpoint$.$anonfun$getBrokerMetadataAndOfflineDirs$2(BrokerMetadataCheckpoint.scala:172)
	at scala.collection.Iterator.foreach(Iterator.scala:943)
	at scala.collection.Iterator.foreach$(Iterator.scala:943)
	at scala.collection.Abstra...

**Before/After**
```java
// Before
from kafkatest.version import LATEST_3_3, LATEST_3_4, LATEST_3_5, \

        # Due to compatability issue with version 3.3, we need to use a single folder. Using multiple folders
        # will cause broker to throw InconsistentBrokerMetadataException during startup.
        # see https://github.com/apache/kafka/pull/13130
        server_prop_overrides = None
        if starting_kafka_version == str(LATEST_3_3):
            server_prop_overrides = [[config_property.LOG_DIRS, "/mnt/kafka/kafka-metadata-logs"], [config_property.METADATA_LOG_DIR, ""]]

                                                       'configs': {"min.insync.replicas": 2}}},
                                  server_prop_overrides = server_prop_overrides)
    @matrix(from_kafka_version=[str(LATEST_3_3), str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(LATEST_3_8), str(LATEST_3_9), str(DEV_BRANCH)],

// After
from kafkatest.version import LATEST_3_4, LATEST_3_5, \
                                                       'configs': {"min.insync.replicas": 2}}})
    @matrix(from_kafka_version=[str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(LATEST_3_8), str(LATEST_3_9), str(DEV_BRANCH)],
    @matrix(from_kafka_version=[str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(LATEST_3_8), str(LATEST_3_9), str(DEV_BRANCH)],
    @matrix(from_kafka_version=[str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(LATEST_3_8), str(LATEST_3_9), str(DEV_BRANCH)],
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18941: do not test 3.3 in upgrade_tests.py (#19162).

**Test**
Test modified in upgrade_test.py covering the corrected behaviour.
