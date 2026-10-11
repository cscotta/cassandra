**Commit**: 24ce8c2852 · **Ticket**: KAFKA-20291
**Subsystem**: tests/kafkatest/tests/client/consumer_test.py
**Category**: Missing null / bounds check

**Root cause**
The system test `test_consumer_failure` was decorated with `@matrix` parameters that included `enable_assignment_batching`, but the method signature did not declare this parameter. When the test framework injected the parameter, Python raised a `TypeError` because the method did not accept it. This caused the test to fail at dispatch time rather than at assertion time, producing a confusing error that looked like a test infrastructure problem.

**Before/After**
```python
# Before: method signature missing enable_assignment_batching parameter
def test_consumer_failure(self, clean_shutdown, enable_autocommit,
                          metadata_quorum=quorum.isolated_kraft,
                          group_protocol=None):

# After: parameter added with default value
def test_consumer_failure(self, clean_shutdown, enable_autocommit,
                          metadata_quorum=quorum.isolated_kraft,
                          group_protocol=None,
                          enable_assignment_batching=True):
```

**How to spot**
When a `@matrix`/`@parametrize` decorator adds a new dimension to a test, verify that the decorated method signature is updated to accept the new parameter. Python's duck typing means the mismatch is only caught at runtime, not at import time.

**Test**
This commit fixes the test signature so the existing `test_consumer_failure` parameterised scenario can actually run. No new test logic was added.
