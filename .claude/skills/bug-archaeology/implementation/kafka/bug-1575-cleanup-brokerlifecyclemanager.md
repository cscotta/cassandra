**Commit**: 892a446789 · **Ticket**: KAFKA-18594
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
  val isZkBroker: Boolean,
    if (isZkBroker) {
      builder.append(" isZkBroker=true")
    }
   * True if we sent a event queue to the active controller requesting controlled
      if (!isZkBroker) {
        registered = false
        scheduleNextCommunicationImmediately()
      }
      if (!isZkBroker) {
        // Only KRaft brokers block on registration during startup
        eventQueue.scheduleDeferred("initialRegistrationTimeout",

// After
   * True if we sent an event queue to the active controller requesting controlled
      registered = false
      scheduleNextCommunicationImmediately()
      eventQueue.scheduleDeferred("initialRegistrationTimeout",
        new DeadlineFunction(time.nanoseconds() + initialTimeoutNs),
        new RegistrationTimeoutEvent())
        setIsMigratingZkBroker(false).
    manager = new BrokerLifecycleManager(context.config, context.time, "create-and-close-", Set(Uuid.fromString("oFoTeS9QT0aAyCyH41v45A")))
    manager = new BrokerLifecycleManager(context.config, context.time, "create-start-and-close-", Set(Uuid.fromString("uiUADXZWTPixVvp6UWFWnw")))
    manager = new BrokerLifecycleManager(context.config, context.time, "successful-registration-", Set(Uuid.fromString("gCpDJgRlS2CBCpxoP2VMsQ")))
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18594: cleanup brokerlifecyclemanager (#18626).

**Test**
Test modified in BrokerLifecycleManagerTest.scala covering the corrected behaviour.
