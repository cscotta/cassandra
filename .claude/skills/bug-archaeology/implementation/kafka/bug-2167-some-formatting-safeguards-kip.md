**Commit**: e3751a838c · **Ticket**: KAFKA-17794
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
KIP-853 adds support for dynamic KRaft quorums. This means that the quorum topology is
no longer statically determined by the controller.quorum.voters configuration. Instead, it
is contained in the storage directories of each controller and broker. Users of dynamic quorums must format at least one controller storage directory with either
the --initial-controllers or --standalone flags.

**Before/After**
```java
// Before
import org.apache.kafka.raft.DynamicVoters
      foreach(v => formatter.setInitialVoters(DynamicVoters.parse(v)))
      formatter.setInitialVoters(createStandaloneDynamicVoters(config))
      throw new TerseFailure("You cannot use --standalone on a broker node.")
      .help("Used to initialize a single-node quorum controller quorum.")
      .help("The initial controllers, as a comma-separated list of id@hostname:port:directory. The same values must be used to format all nodes. For example:\n" +
        "0@example.com:8082:JEXY6aqzQY-32P5TStzaFg,1@example.com:8083:MvDxzVmcRsaTz33bUuRU6A,2@example.com:8084:07R5amHmR32VDA6jHkGbTA\n")
  defaultDynamicQuorumProperties.setProperty("controller.quorum.vot

// After
import org.apache.kafka.raft.{DynamicVoters, QuorumConfig}
      foreach(v => formatter.setInitialControllers(DynamicVoters.parse(v)))
      formatter.setInitialControllers(createStandaloneDynamicVoters(config))
    }
    if (!namespace.getBoolean("no_initial_controllers")) {
      if (config.processRoles.contains(ProcessRole.ControllerRole)) {
        if (config.quorumConfig.voters().isEmpty) {
          if (formatter.initialVoters().isEmpty()) {
            throw new TerseFailure("Because " + QuorumConfig.QUORUM_VOTERS_CONFIG +
              " is not set on this controller, you must specify one of the following: " +
              "--standalone, --initial-controllers, or --no-initial-controllers.");
          }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17794: add some formatting safeguards for kip-853 (#17504).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
