**Commit**: 80eb86e18e · **Ticket**: KAFKA-19418
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Fix to avoid flakiness in verifiable producer system test. The test
lists running processes and greps to find the VerifiableProducer one,
but wasn't providing an specific pattern to grep (so flaky if there were
more than one process containing the default grep pattern "kafka")

Fix by passing a "proc_grep_string"  to filter when looking for the
VerifiableProducer process. All test pass successfully after the change. Reviewers: PoAn Yang <payang@apache.org>, Andrew Schofield
<aschofield@confluent.io>

**Before/After**
```java
// Before
        assert is_version(node, [node.version.vstring], logger=self.logger)
        # See above comment above regarding use of version.vstring (distutils.version.LooseVersion)
        assert is_version(node, [node.version.vstring], logger=self.logger)
        # See above comment above regarding use of version.vstring (distutils.version.LooseVersion)
        assert is_version(node, [node.version.vstring], logger=self.logger)

// After
        assert is_version(node, [node.version.vstring], proc_grep_string=VerifiableProducer.__qualname__, logger=self.logger)
        # See comment above regarding use of version.vstring (distutils.version.LooseVersion)
        assert is_version(node, [node.version.vstring], proc_grep_string=VerifiableProducer.__qualname__, logger=self.logger)
        # See comment above regarding use of version.vstring (distutils.version.LooseVersion)
        assert is_version(node, [node.version.vstring], proc_grep_string=VerifiableProducer.__qualname__, logger=self.logger)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19418: fix for flaky verifiable producer system test (#20001).

**Test**
Test modified in test_verifiable_producer.py covering the corrected behaviour.
