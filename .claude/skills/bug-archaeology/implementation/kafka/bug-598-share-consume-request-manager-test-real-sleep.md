**Commit**: 679854c9b6 · **Ticket**: KAFKA-19896

**Subsystem**: clients/src/test/java/org/apache/kafka/clients/consumer/internals/ShareConsumeRequestManagerTest.java

**Category**: Race condition/missing synchronization

**Root cause**
Four unit tests in `ShareConsumeRequestManagerTest` that exercised acknowledgement retry after a backoff period used real wall-clock `Thread.sleep()` to advance time past the backoff window. This made the tests slow and subject to timing jitter on loaded CI machines. Since `ShareConsumeRequestManager` consults a `MockTime` instance in tests, calling `time.sleep()` to advance the mock clock is sufficient to expire the backoff without any actual waiting.

**Before**
```java
// Before: real sleep to expire backoff
Thread.sleep(backoffMs + 50);
```

**After**
```java
// After: advance MockTime instead
time.sleep(backoffMs + 50);
```

**How to spot**
Search `ShareConsumeRequestManagerTest` (and similar request-manager tests) for `Thread.sleep`; any such call in a test that also holds a `MockTime` reference can be replaced with `time.sleep` to make the test deterministic and fast.

**Test**
Run all four affected tests after the change and confirm they complete in under one second each without `Thread.sleep`.
