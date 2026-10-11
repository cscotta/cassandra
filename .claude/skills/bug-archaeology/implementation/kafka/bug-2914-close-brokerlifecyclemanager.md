**Commit**: b8b248b1e8 · **Ticket**: KAFKA-16920
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Tests in BrokerLifecycleManagerTest do not close BrokerLifecycleManager
if an assertion fails. This change makes BrokerLifecycleManager an instance variable that is
closed in an `@AfterEach` method. Reviewers: Igor Soarez <i@soarez.me>

**Before/After**
```java
// Before
import org.junit.jupiter.api.{Test, Timeout}
    val manager = new BrokerLifecycleManager(context.config, context.time, "create-and-close-", isZkBroker = false, Set(Uuid.fromString("oFoTeS9QT0aAyCyH41v45A")))
    val manager = new BrokerLifecycleManager(context.config, context.time, "create-start-and-close-", isZkBroker = false, Set(Uuid.fromString("uiUADXZWTPixVvp6UWFWnw")))
    val manager = new BrokerLifecycleManager(context.config, context.time, "successful-registration-", isZkBroker = false, Set(Uuid.fromString("gCpDJgRlS2CBCpxoP2VMsQ")))
    manager.close()
    val manager = new BrokerLifecycleManager(context.config, context.time, "registration-timeout-", isZkBroker = false, Set(Uuid.fromString("9XBOAtr4T0Wbx2sbiWh6xg")))
    manager.close()
    val manager = new BrokerLifecycleManager(context.config, context.time, "controlled-shutdown-", isZkBroker = false, Set(Uuid.fromString("B4RtUz1ySGip3A7ZFYB2dg")))
    manager.close()
    val manager = new BrokerLifecycleManager(ctx.config, ctx.time, "offline-dirs-sent-in-heartbeat-", isZkBroker = false, Set(Uuid.fromString("0IbF1sjhSGG6FNvnrPbqQg")))

// After
import org.junit.jupiter.api.{AfterEach, Test, Timeout}
  private var manager: BrokerLifecycleManager = null

  @AfterEach
  def tearDown(): Unit = {
    if (manager != null) {
      manager.close()
    }
  }

    manager = new BrokerLifecycleManager(context.config, context.time, "create-and-close-", isZkBroker = false, Set(Uuid.fromString("oFoTeS9QT0aAyCyH41v45A")))
    manager = new BrokerLifecycleManager(context.config, context.time, "create-start-and-close-", isZkBroker = false, Set(Uuid.fromString("uiUADXZWTPixVvp6UWFWnw")))
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16920: close brokerlifecyclemanager in tests (#16252).

**Test**
Test modified in BrokerLifecycleManagerTest.scala covering the corrected behaviour.
