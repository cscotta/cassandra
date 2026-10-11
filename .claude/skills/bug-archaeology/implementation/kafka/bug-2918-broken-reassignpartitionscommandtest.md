**Commit**: 5a5a292146 · **Ticket**: KAFKA-16606
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
KAFKA-16606 (#15834) introduced a change that broke
ReassignPartitionsCommandTest.testReassignmentCompletionDuringPartialUpgrade. The point was to validate that the MetadataVersion supports JBOD
in KRaft when multiple log directories are configured. We do that by checking the version used in
kafka-features.sh upgrade --metadata, and the version discovered
via a FeatureRecord for metadata.version in the cluster metadata. There's no point in checking inter.broker.protocol.version in
KafkaConfig, since in KRaft, that configuration is deprecated
and ignored — always assuming the value of MINIMUM_KRAFT_VERSION.

**Before/After**
```java
// Before
    if (processRoles.contains(ProcessRole.BrokerRole)
      && originals.containsKey(ReplicationConfigs.INTER_BROKER_PROTOCOL_VERSION_CONFIG)
      && logDirs.size > 1) {
        require(interBrokerProtocolVersion.isDirectoryAssignmentSupported,
          s"Multiple log directories (aka JBOD) are not supported with the configured " +
            s"${interBrokerProtocolVersion} ${ReplicationConfigs.INTER_BROKER_PROTOCOL_VERSION_CONFIG}. " +
            s"Need ${MetadataVersion.IBP_3_7_IV2} or higher")
    }

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: fix broken reassignpartitionscommandtest test (#16251).

**Test**
No test added.
