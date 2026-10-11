**Commit**: 2daf5be27a · **Ticket**: KAFKA-16024
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
- `broker.session.timeout.ms` defaults to 9s. When a broker goes
offline, the group coordinator may take up to this long to be
re-elected. - The commit callback retry timeout is currently 10 seconds, which
leaves very little buffer. If the metadata hasn’t refreshed yet, the
consumer may still send an OFFSET_COMMIT request to the offline
coordinator, leading to transient failures.

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.server.config.ServerConfigs
import org.junit.jupiter.api.{BeforeEach, TestInfo}
  private var currentTestName: String = _

  @BeforeEach
  override def setUp(testInfo: TestInfo): Unit = {
    currentTestName = testInfo.getTestMethod.get().getName
    super.setUp(testInfo)
  }

  override protected def brokerPropertyOverrides(properties: Properties): Unit = {
    super.brokerPropertyOverrides(properties)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16024: saslplaintextconsumertest#testcoordinatorfailover is flaky (#20774).

**Test**
Test modified in BaseConsumerTest.scala covering the corrected behaviour.
