**Commit**: 804bf596a0 · **Ticket**: KAFKA-18329
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This patch is the first of a series of patches to remove the old group
coordinator. With the release of Apache Kafka 4.0, the so-called new
group coordinator is the default and only option available now. This patch update the system tests to not run with the old group
coordinator. It also removed the ability to use the old group
coordinator.

**Before/After**
```java
// Before
NEW_GROUP_COORDINATOR_ENABLE="group.coordinator.new.enable"

  /** New group coordinator configs */
  val NewGroupCoordinatorEnableProp = "group.coordinator.new.enable"
                 use_new_coordinator=None,
        :param use_new_coordinator: When true, use the new implementation of the group coordinator as per KIP-848. If this is None, the default existing group coordinator is used.
        # Set use_new_coordinator based on context and arguments.
        # If not specified, the default config is used.
        if use_new_coordinator is None:
            arg_name = 'use_new_coordinator'
            if context.injected_args is not None:
                use_new_coordinator = context.injected_args.get(arg_name)

// After
        metadata_quorum=[quorum.isolated_kraft]
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18329; [1/3] delete old group coordinator (kip-848) (#19243).

**Test**
Test modified in config_property.py covering the corrected behaviour.
