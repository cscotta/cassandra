**Commit**: 2dc3ee0557 · **Ticket**: KAFKA-17497
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
from ducktape.mark import parametrize
from kafkatest.services.kafka import KafkaService
from kafkatest.version import DEV_BRANCH, LATEST_3_4
    def do_migration(self, roll_controller = False, downgrade_to_zk = False):
        controller = KafkaService(self.test_context, num_nodes=1, zk=self.zk, version=DEV_BRANCH,
    @parametrize(roll_controller = True)
    @parametrize(roll_controller = False)
    def test_online_migration(self, roll_controller):
                                  server_prop_overrides=[
                                      ["zookeeper.metadata.migration.enable", "false"],
                                  ])
        self.run_produce_consume_validate(core_test_action=partial(self.do_migration, roll_controller = roll_controller))

// After
from ducktape.mark import parametrize, matrix
from kafkatest.services.kafka import KafkaService, config_property
from kafkatest.version import DEV_BRANCH, LATEST_3_4, LATEST_3_7, KafkaVersion
    def do_migration(self, roll_controller=False, downgrade_to_zk=False, from_kafka_version=str(DEV_BRANCH)):
        controller = KafkaService(self.test_context, num_nodes=1, zk=self.zk, version=KafkaVersion(from_kafka_version),
    @matrix(roll_controller=[True, False], from_kafka_version=[str(DEV_BRANCH), str(LATEST_3_7)])
    def test_online_migration(self, roll_controller, from_kafka_version):

        server_prop_overrides = [["zookeeper.metadata.migration.enable", "false"]]

        if from_kafka_version != str(DEV_BRANCH):
            server_prop_overrides.append([config_property.INTER_BROKER_PROTOCOL_VERSION, from_kafka_version])
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17497 add e2e for zk migration with old controller (#17131).

**Test**
Test modified in zookeeper_migration_test.py covering the corrected behaviour.
