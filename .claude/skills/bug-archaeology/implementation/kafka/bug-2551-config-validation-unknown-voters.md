**Commit**: 20c3e7324b · **Ticket**: KAFKA-16842
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This change fixes the Kafka configuration validation to take into account the reconfiguration changes to configuration and allows KRaft observers to start with an unknown set of voters. For the Kafka configuration validation the high-level change is that now the user only needs to specify either the controller.quorum.bootstrap.servers property or the controller.quorum.voters property. The other notable change in the configuration is that controller listeners can now be (and should be) specified in advertise.listeners property. Because Kafka can now be configured without any voters and just the bootstrap servers.

**Before/After**
```java
// Before
	  // be explicit about the reload4j version instead of relying on the transitive versions
	  libs.reload4j

  //  the task `removeUnusedImports` is implemented by google-java-format,
  //  and unfortunately the google-java-format version used by spotless 6.14.0 can't work with JDK 21.
    // log4j-appender is not in core dependencies,

    def validateNonEmptyQuorumVotersForKRaft(): Unit = {
      if (voterIds.isEmpty) {
        throw new ConfigException(s"If using ${KRaftConfigs.PROCESS_ROLES_CONFIG}, ${QuorumConfig.QUORUM_VOTERS_CONFIG} must contain a parseable set of voters.")
    def validateNonEmptyQuorumVotersForMigration(): Unit = {
      if (voterIds.isEmpty) {

// After
          // be explicit about the reload4j version instead of relying on the transitive versions
          libs.reload4j

  //  the task `removeUnusedImports` is implemented by google-java-format,
  //  and unfortunately the google-java-format version used by spotless 6.14.0 can't work with JDK 21.
    // log4j-appender is not in core dependencies,

    testImplementation libs.hamcrest
    def validateQuorumVotersAndQuorumBootstrapServerForKRaft(): Unit = {
      if (voterIds.isEmpty && quorumBootstrapServers.isEmpty) {
        throw new ConfigException(
          s"""If using ${KRaftConfigs.PROCESS_ROLES_CONFIG}, either ${QuorumConfig.QUORUM_BOOTSTRAP_SERVERS_CONFIG} must
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16842; fix config validation and support unknown voters (#16892).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
