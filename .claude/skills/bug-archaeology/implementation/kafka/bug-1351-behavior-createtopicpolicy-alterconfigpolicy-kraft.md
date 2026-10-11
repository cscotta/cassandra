**Commit**: 011f256c86 · **Ticket**: KAFKA-18886
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
1. Updated JavaDoc to reflect that CreateTopicPolicy and AlterConfigPolicy run on the controller in KRaft mode. 2. Modified Behavioral Change Reference in the HTML docs to include this change.

**Before/After**
```java
// Before
            "implement the <code>org.apache.kafka.server.policy.CreateTopicPolicy</code> interface.";
            "implement the <code>org.apache.kafka.server.policy.AlterConfigPolicy</code> interface.";

// After
    def warnIfConfigDefinedInWrongRole(expectedRole: ProcessRole, configName: String): Unit = {
      if (originals.containsKey(configName)) {
        warn(s"$configName is defined in ${processRoles.mkString(", ")}. It should be defined in the $expectedRole role.")
      }
    }
      // warn if create.topic.policy.class.name or alter.config.policy.class.name is defined in the broker role
      warnIfConfigDefinedInWrongRole(ProcessRole.ControllerRole, ServerLogConfigs.CREATE_TOPIC_POLICY_CLASS_NAME_CONFIG)
      warnIfConfigDefinedInWrongRole(ProcessRole.ControllerRole, ServerLogConfigs.ALTER_CONFIG_POLICY_CLASS_NAME_CONFIG)
        <li>
            <strong>Policy Class Deployment</strong>:
            In KRaft mode, the <code>CreateTopicPolicy</code> and <code>AlterConfigPolicy</code> plugins run on the controller instead of the broker.
            This requires users to deploy the policy class JAR files on the controller and configure the parameters
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18886 add behavior change of createtopicpolicy and alterconfigpolicy to zk2kraft (#19087).

**Test**
No test added.
