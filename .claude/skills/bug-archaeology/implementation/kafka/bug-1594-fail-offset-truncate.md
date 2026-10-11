**Commit**: 78e35457ee · **Ticket**: KAFKA-18480
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

from kafkatest.services.kafka import TopicPartition
    def test_offset_truncate(self):

// After
from ducktape.mark import matrix
from kafkatest.services.kafka import TopicPartition, quorum
    @matrix(metadata_quorum=quorum.all_non_upgrade, use_new_coordinator=[True])
    def test_offset_truncate(self, metadata_quorum, use_new_coordinator):
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18480 fix fail e2e `test_offset_truncate` (#18492).

**Test**
Test modified in truncation_test.py covering the corrected behaviour.
