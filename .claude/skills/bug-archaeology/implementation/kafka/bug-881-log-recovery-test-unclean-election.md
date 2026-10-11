**Commit**: a61a37f7dd · **Ticket**: KAFKA-19452

**Subsystem**: Replication / LogRecoveryTest

**Category**: Flaky test

**Root cause**
`LogRecoveryTest` exercised log recovery after an unclean shutdown, which can require electing a replica that is not fully caught up (unclean leader election). In KRaft mode, unclean leader election is disabled by default in the controller configuration, causing the test to hang or fail intermittently when no clean leader candidate was available. The fix explicitly enables `unclean.leader.election.enable` in the KRaft controller configuration for this test.

**Before/After**
```java
// Before: relies on default controller config (unclean election disabled)
Properties controllerProps = new Properties();
// unclean.leader.election.enable not set → defaults to false

// After: explicitly enable for this test
Properties controllerProps = new Properties();
controllerProps.put("unclean.leader.election.enable", "true");
```

**How to spot**
Recovery tests that depend on electing a lagging replica should explicitly configure `unclean.leader.election.enable=true` in their embedded controller setup; relying on defaults causes different behavior between ZooKeeper and KRaft modes.

**Test**
`LogRecoveryTest` passes reliably in KRaft mode after enabling unclean leader election in the controller properties.
