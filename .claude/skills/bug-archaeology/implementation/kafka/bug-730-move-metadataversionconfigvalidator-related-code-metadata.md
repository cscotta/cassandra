**Commit**: e27ea8d4db · **Ticket**: KAFKA-19702
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
1. Move `MetadataVersionConfigValidator` to metadata module. 2. Move `MetadataVersionConfigValidatorTest` to metadata module.

**Before/After**
```java
// Before
import org.apache.kafka.metadata.{BrokerState, ListenerInfo}
      metadataPublishers.add(new MetadataVersionConfigValidator(config, sharedServer.metadataPublishingFaultHandler))
import org.apache.kafka.server.common.MetadataVersion
  /**
   * Validate some configurations for new MetadataVersion. A new MetadataVersion can take place when
   * a FeatureLevelRecord for "metadata.version" is read from the cluster metadata.
   */
  def validateWithMetadataVersion(metadataVersion: MetadataVersion): Unit = {
    if (processRoles.contains(ProcessRole.BrokerRole) && logDirs.size > 1) {
      require(metadataVersion.isDirectoryAssignmentSupported,
        s"Multiple log directories (aka JBOD) are not supported in the current MetadataVersion ${metadataVersion}. " +
          s"Need ${MetadataVersion.IBP_3_7_IV2} or higher")

// After
import org.apache.kafka.metadata.{BrokerState, ListenerInfo, MetadataVersionConfigValidator}
      metadataPublishers.add(new MetadataVersionConfigValidator(config.brokerId,
        () => config.processRoles.contains(ProcessRole.BrokerRole) && config.logDirs().size() > 1,
        sharedServer.metadataPublishingFaultHandler
      ))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19702 move metadataversionconfigvalidator and related test code to metadata module (#20526).

**Test**
Test modified in MetadataVersionConfigValidatorTest.java covering the corrected behaviour.
