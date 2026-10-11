**Commit**: deab703e43 · **Ticket**: KAFKA-17292
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add the "share" group coordinator rebalance protocol as the way to enable KIP-932. It is also necessary to turn on the new group coordinator. Reviewers:  Apoorv Mittal <apoorvmittal10@gmail.com>,  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    // Only classic and consumer are supported.
    val config = KafkaConfig.fromProps(props)
            "The " + Group.GroupType.CONSUMER + " rebalance protocol is in early access and therefore must not be used in production.";

// After
    if (protocols.contains(GroupType.SHARE)) {
      // The CONSUMER protocol enables the new group coordinator, and that's a prerequisite for share groups.
      if (!protocols.contains(GroupType.CONSUMER)) {
        throw new ConfigException(s"Enabling the new '${GroupType.SHARE}' rebalance protocol requires '${GroupType.CONSUMER}' to be enabled also.")
      }
      warn(s"Share groups and the new '${GroupType.SHARE}' rebalance protocol are enabled. " +
        "This is part of the early access of KIP-932 and MUST NOT be used in production.")
    }
    // Only classic, consumer and share are supported.
    var config = KafkaConfig.fromProps(props)
    assertFalse(config.shareGroupConfig.isShareGroupEnabled)

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17292: introduce share coordinator protocol config (#16847).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
