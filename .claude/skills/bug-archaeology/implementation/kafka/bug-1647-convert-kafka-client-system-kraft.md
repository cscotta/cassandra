**Commit**: 45e3c21e9a · **Ticket**: KAFKA-17915
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    def test_broker_rolling_bounce(self, metadata_quorum=quorum.zk, use_new_coordinator=False, group_protocol=None):
    def test_consumer_bounce(self, clean_shutdown, bounce_mode, metadata_quorum=quorum.zk, use_new_coordinator=False, group_protocol=None):
    def test_static_consumer_bounce_with_eager_assignment(self, clean_shutdown, static_membership, bounce_mode, num_bounces, metadata_quorum=quorum.zk, use_new_coordinator=False, group_protocol=None):
    def test_static_consumer_persisted_after_rejoin(self, bounce_mode, metadata_quorum=quorum.zk, use_new_coordinator=False, group_protocol=None):
    def test_fencing_static_consumer(self, num_conflict_consumers, fencing_stage, metadata_quorum=quorum.zk, use_new_coordinator=False, group_protocol=None):
    def test_consumer_failure(self, clean_shutdown, enable_autocommit, metadata_quorum=quorum.zk, use_new_coordinator=False, group_protocol=None):
    def test_broker_failure(self, clean_shutdown, enable_autocommit, metadata_quorum=quorum.zk, use_new_coordinator=False, group_protocol=None):
    def test_group_consumption(self, metadata_quorum=quorum.zk, use_new_coordinator=False, group_protocol=None):

// After
    def test_broker_rolling_bounce(self, metadata_quorum=quorum.isolated_kraft, use_new_coordinator=False, group_protocol=None):
    def test_consumer_bounce(self, clean_shutdown, bounce_mode, metadata_quorum=quorum.isolated_kraft, use_new_coordinator=False, group_protocol=None):
    def test_static_consumer_bounce_with_eager_assignment(self, clean_shutdown, static_membership, bounce_mode, num_bounces, metadata_quorum=quorum.isolated_kraft, use_new_coordinator=False, group_protocol=None):
    def test_static_consumer_persisted_after_rejoin(self, bounce_mode, metadata_quorum=quorum.isolated_kraft, use_new_coordinator=False, group_protocol=None):
    def test_fencing_static_consumer(self, num_conflict_consumers, fencing_stage, metadata_quorum=quorum.isolated_kraft, use_new_coordinator=False, group_protocol=None):
    def test_consumer_failure(self, clean_shutdown, enable_autocommit, metadata_quorum=quorum.isolated_kraft, use_new_coordinator=False, group_protocol=None):
    def test_broker_failure(self, clean_shutdown, enable_autocommit, metadata_quorum=quorum.isolated_kraft, use_new_coordinator=False, group_protocol=None):
    def test_group_consumption(self, metadata_quorum=quorum.isolated_kraft, use_new_coordinator=False, group_protocol=None):
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17915: convert kafka client system tests to use kraft (#17669).

**Test**
Test modified in consumer_test.py covering the corrected behaviour.
