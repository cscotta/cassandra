**Commit**: dde0b8cd92 · **Ticket**: KAFKA-19042
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
PlaintextConsumerTest should extend AbstractConsumerTest instead
BaseConsumerTest. Otherwise, those tests will be executed on both
`clients-integration-tests` and `core` (see
https://github.com/apache/kafka/pull/20081/files#r2190749592). Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
@Timeout(600)
class PlaintextConsumerTest extends BaseConsumerTest {

// After
@Timeout(60)
class PlaintextConsumerTest extends AbstractConsumerTest {
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: prevent unnecessary test runs - kafka-19042 follow-up (#20122).

**Test**
Test modified in PlaintextConsumerTest.scala covering the corrected behaviour.
