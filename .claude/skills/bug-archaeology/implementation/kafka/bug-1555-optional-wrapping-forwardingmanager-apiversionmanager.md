**Commit**: 084fcbd327 · **Ticket**: KAFKA-18599
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
`forwardingManager` is always present now. Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
    forwardingManager: Option[ForwardingManager],
  forwardingManager: Option[ForwardingManager],
    val controllerApiVersions = forwardingManager.flatMap(_.controllerApiVersions)
        Some(forwardingManager),
import org.junit.jupiter.api.{Disabled, Test}
      forwardingManager = None,
      forwardingManager = None,
      forwardingManager = Some(forwardingManager),
    for (forwardingManagerOpt <- Seq(Some(forwardingManager), None)) {
      val versionManager = new DefaultApiVersionManager(
        listenerType = ListenerType.BROKER,
        forwardingManager = forwardingManagerOpt,

// After
    forwardingManager: ForwardingManager,
  forwardingManager: ForwardingManager,
    val controllerApiVersions = forwardingManager.controllerApiVersions
        forwardingManager,
import org.junit.jupiter.api.Test
    val forwardingManager = Mockito.mock(classOf[ForwardingManager])
      forwardingManager = forwardingManager,
    val forwardingManager = Mockito.mock(classOf[ForwardingManager])
      forwardingManager = forwardingManager,
      forwardingManager = forwardingManager,
      listenerType = ListenerType.BROKER,
      forwarding
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18599: remove optional wrapping for forwardingmanager in apiversionmanager (#18630).

**Test**
Test modified in ApiVersionManagerTest.scala covering the corrected behaviour.
