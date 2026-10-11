**Commit**: 36c131ef4a · **Ticket**: KAFKA-17609
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This is part one of a multi-pr effort to convert Kafka Streams system tests to KRaft. I decided to break down the changes into multiple PRs to reduce the review load

Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
from kafkatest.version import KafkaVersion, LATEST_0_10_0, LATEST_0_10_1

        if self.KAFKA_STREAMS_VERSION == str(LATEST_0_10_0) or self.KAFKA_STREAMS_VERSION == str(LATEST_0_10_1):
            args['zk'] = self.kafka.zk.connect_setting()
        else:
            args['zk'] = ""
              " %(kafka_run_class)s %(streams_class_name)s %(zk)s %(config_file)s " \

        if self.KAFKA_STREAMS_VERSION == str(LATEST_0_10_0) or self.KAFKA_STREAMS_VERSION == str(LATEST_0_10_1):
            args['zk'] = self.kafka.zk.connect_setting()
        else:
            args['zk'] = ""

// After
              " %(kafka_run_class)s %(streams_class_name)s %(config_file)s " \
              " %(kafka_run_class)s %(streams_class_name)s %(config_file)s " \
    def __init__(self, test_context,  topics, num_controllers=1, num_brokers=3):
        super(BaseStreamsTest, self).__init__(test_context, num_controllers, num_brokers, topics)
from kafkatest.services.kafka import KafkaService, quorum
    @cluster(num_nodes=9)
    @matrix(from_version=smoke_test_versions, bounce_type=["full"], metadata_quorum=[quorum.combined_kraft])
    def test_app_upgrade(self, from_version, bounce_type, metadata_quorum):
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17609:[1/4] changes needed to convert system tests to use kraft and remove zk  (#17275).

**Test**
Test modified in streams.py covering the corrected behaviour.
