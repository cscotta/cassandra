**Commit**: ceec218351 · **Ticket**: KAFKA-16949
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
from kafkatest.services.connect import ConnectDistributedService, VerifiableSource, VerifiableSink, ConnectRestError, MockSink, MockSource
        self.cc.start()

// After
from kafkatest.services.connect import ConnectDistributedService, ConnectServiceBase, VerifiableSource, VerifiableSink, ConnectRestError, MockSink, MockSource
        self.cc.start(mode=ConnectServiceBase.STARTUP_MODE_JOIN)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16949: fixing test_dynamic_logging in system test connect_distributed_test (#15915).

**Test**
Test modified in connect_distributed_test.py covering the corrected behaviour.
