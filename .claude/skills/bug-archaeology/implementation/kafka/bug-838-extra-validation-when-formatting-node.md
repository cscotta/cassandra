**Commit**: 1bcaa19c46 · **Ticket**: KAFKA-19489
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR adds a check to the storage tool's format command which throws a
TerseFailure when the controller.quorum.voters config is defined and the
node is formatted with the --standalone flag or the
--initial-controllers flag. Without this check, it is possible to have two voter sets. For example,
in a three node setup, the two nodes that formatted with
--no-initial-controllers could form quorum with each other since they
have the static voter set, and the --standalone node would ignore the
config and read the voter set of itself from its log, forming its own
quorum of 1. Reviewers: José Armando García Sancio <jsancio@apache.org>, TaiJuWu
 <tjwu1217@gmail.com>, Alyssa Huang <ahuang@confluent....

**Before/After**
```java
// Before
    Option(namespace.getString("initial_controllers")).
    if (namespace.getBoolean("standalone")) {
    properties.putAll(defaultStaticQuorumProperties)

// After
    val initialControllers = namespace.getString("initial_controllers")
    val isStandalone = namespace.getBoolean("standalone")
    if (!config.quorumConfig.voters().isEmpty &&
      (Option(initialControllers).isDefined || isStandalone)) {
      throw new TerseFailure("You cannot specify " +
        QuorumConfig.QUORUM_VOTERS_CONFIG + " and format the node " +
        "with --initial-controllers or --standalone. " +
        "If you want to use dynamic quorum, please remove " +
        QuorumConfig.QUORUM_VOTERS_CONFIG + " and specify " +
        QuorumConfig.QUORUM_BOOTSTRAP_SERVERS_CONFIG + " instead.")
    }
    Option(initialControllers).
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19489; extra validation when formatting a node (#20136).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
