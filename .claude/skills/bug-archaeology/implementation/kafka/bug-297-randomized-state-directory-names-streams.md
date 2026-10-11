**Commit**: 6fe8415a16 · **Ticket**: KAFKA-9195
**Subsystem**: tests/kafkatest
**Category**: State not cleaned up

**Root cause**
This PR fixes Streams system test state directory isolation

Currently, Streams system tests set state.dir to PERSISTENT_ROOT
(/mnt/streams). Since the state directory path is state.dir +
application.id + task.id, tests with the same application ID can
interfere with each other if state directories aren't properly cleaned
up, leading to flaky failures. This PR sets state.dir to a randomized path inside PERSISTENT_ROOT
(/mnt/streams/<uuid>) to ensure test isolation even when state cleanup
is missed. Reviewers: Matthias J.

**Before/After**
```java
// Before
        cfg = KafkaConfig(**{streams_property.STATE_DIR: self.PERSISTENT_ROOT, streams_property.KAFKA_SERVERS: self.kafka.bootstrap_servers()})
        properties = {streams_property.STATE_DIR: self.PERSISTENT_ROOT,
        properties = {streams_property.STATE_DIR: self.PERSISTENT_ROOT,
        properties = {streams_property.STATE_DIR: self.PERSISTENT_ROOT,
        properties[streams_property.STATE_DIR] = self.PERSISTENT_ROOT
        properties = {streams_property.STATE_DIR: self.PERSISTENT_ROOT,
        properties = {streams_property.STATE_DIR: self.PERSISTENT_ROOT,
        properties = {streams_property.STATE_DIR: self.PERSISTENT_ROOT,

// After
import uuid
        # Create a randomized state directory path to prevent test interference
        self.state_dir = os.path.join(self.PERSISTENT_ROOT, str(uuid.uuid4()))
        cfg = KafkaConfig(**{streams_property.STATE_DIR: self.state_dir, streams_property.KAFKA_SERVERS: self.kafka.bootstrap_servers()})
        properties = {streams_property.STATE_DIR: self.state_dir,
        properties = {streams_property.STATE_DIR: self.state_dir,
        properties = {streams_property.STATE_DIR: self.state_dir,
        properties[streams_property.STATE_DIR] = self.state_dir
        properties = {streams_property.STATE_DIR: self.state_dir,
        properties = {streams_property.STATE_DIR: self.state_dir,
        properties = {streams_property.STATE_DIR: self.state_dir,
```

**How to spot**
When reviewing tests/kafkatest code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-9195: use randomized state directory names in streams system tests (#21879).

**Test**
Test modified in streams.py covering the corrected behaviour.
