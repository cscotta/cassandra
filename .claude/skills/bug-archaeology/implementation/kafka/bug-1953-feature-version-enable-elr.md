**Commit**: 2b2b3cd355 · **Ticket**: KAFKA-18062
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Replace the ELR static config with feature version. Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
          setInterBrokerListenerName(config.interBrokerListenerName.value()).
          setEligibleLeaderReplicasEnabled(config.elrEnabled)
  val elrEnabled: Boolean = getBoolean(KRaftConfigs.ELR_ENABLED_CONFIG)

import org.apache.kafka.server.common.{MetadataVersion, TransactionVersion}
import org.apache.kafka.server.common.{GroupVersion, MetadataVersion, TransactionVersion}
      assertEquals(3, apiVersionsResponse.data().finalizedFeatures().size())

// After
          setInterBrokerListenerName(config.interBrokerListenerName.value())
import org.apache.kafka.server.common.{EligibleLeaderReplicasVersion, MetadataVersion, TransactionVersion}
    val elrVersion =
      if (TestInfoUtils.isEligibleLeaderReplicasV1Enabled(testInfo)) {
        EligibleLeaderReplicasVersion.ELRV_1.featureLevel()
      } else EligibleLeaderReplicasVersion.ELRV_0.featureLevel()
    formatter.setFeatureLevel(EligibleLeaderReplicasVersion.FEATURE_NAME, elrVersion)


  /**
   * Returns whether eligible leader replicas version 1 is enabled.
   * When no parameter is provided, the default returned is false.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18062: use feature version to enable elr (#17867).

**Test**
Test modified in QuorumTestHarness.scala covering the corrected behaviour.
