**Commit**: 857b1e92cc · **Ticket**: KAFKA-19719
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Just because a controller node sets --no-initial-controllers flag does
not mean it is necessarily running kraft.version=1. The more precise
meaning is that the controller node being formatted does not know what
kraft version the cluster should be in, and therefore it is only safe to
assume kraft.version=0. Only by setting
--standalone,--initial-controllers, or --no-initial-controllers
AND not specifying the controller.quorum.voters static config, is it
known kraft.version > 0. For example, it is a valid configuration (although confusing) to run a
static   quorum defined by controller.quorum.voters but have all the
controllers   format with --no-initial-controllers.

**Before/After**
```java
// Before
    if (!config.quorumConfig.voters().isEmpty &&
      (Option(initialControllers).isDefined || isStandalone)) {
    if (namespace.getBoolean("no_initial_controllers")) {
      formatter.setNoInitialControllersFlag(true)
    } else {
      if (config.processRoles.contains(ProcessRole.ControllerRole)) {
        if (config.quorumConfig.voters().isEmpty && formatter.initialVoters().isEmpty) {
        }
      }
      .help("Used to initialize a controller as a single-node dynamic quorum.")
      .help("Used to initialize a server without a dynamic quorum topology.")
      .help("Used to initialize a server with a specific dynamic quorum topology. The argument " +

// After
    val staticVotersEmpty = config.quorumConfig.voters().isEmpty
    formatter.setHasDynamicQuorum(staticVotersEmpty)
    if (!staticVotersEmpty && (Option(initialControllers).isDefined || isStandalone)) {
    if (!namespace.getBoolean("no_initial_controllers") &&
      config.processRoles.contains(ProcessRole.ControllerRole) &&
      staticVotersEmpty &&
      formatter.initialVoters().isEmpty) {
      .help("Used to initialize a controller as a single-node dynamic quorum. When setting this flag, " +
        "the controller.quorum.voters config must not be set, and controller.quorum.bootstrap.servers is set instead.")
      .help("Used to initialize a server without specifying a dynamic quorum. When setting this flag, " +
        "the controller.quorum.voters config should not be set, and controller.quorum.bootstrap.servers is set instead.")
      .help("Used to initialize a server with the specified dynamic quorum. The argument " +
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19719: --no-initial-controllers should not assume kraft.version=1 (#20551).

**Test**
Test modified in ReconfigurableQuorumIntegrationTest.java covering the corrected behaviour.
