**Commit**: 0bb2aee838 · **Ticket**: KAFKA-17305
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When a broker tries to register with the controller quorum, its registration should be rejected if it doesn't support a feature that is currently enabled. (A feature is enabled if it is set to a non-zero feature level.) This is important for the newly added kraft.version feature flag. Reviewers: Colin P. McCabe <cmccabe@apache.org>, José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.feature.SupportedVersionRange
import org.apache.kafka.metadata.{BrokerState, ListenerInfo, VersionRange}
      val featuresRemapped = brokerFeatures.supportedFeatures.features().asScala.map {
        case (k: String, v: SupportedVersionRange) =>
          k -> VersionRange.of(v.min, v.max)
      }.asJava
          // Even though ZK brokers don't use "metadata.version" feature, we send our IBP here as part of the broker registration
          val ibpAsFeature =
           java.util.Collections.singletonMap(MetadataVersion.FEATURE_NAME,
             VersionRange.of(config.interBrokerProtocolVersion.featureLevel(), config.interBrokerProtocolVersion.featureLevel()))
            ibpAsFeature,

// After
import org.apache.kafka.metadata.VersionRange
  def createDefaultFeatureMap(features: BrokerFeatures): Map[String, VersionRange] = {
    features.supportedFeatures.features.asScala.map {
      case (name, versionRange) =>
        (name, VersionRange.of(versionRange.min, versionRange.max))
    }.toMap
  }

import org.apache.kafka.metadata.{BrokerState, ListenerInfo}
      val featuresRemapped = BrokerFeatures.createDefaultFeatureMap(brokerFeatures).asJava
          val features = BrokerFeatures.createDefaultFeatureMap(BrokerFeatures.createDefault(config.unstableFeatureVersionsEnabled))

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17305; check broker registrations for missing features (#16848).

**Test**
Test modified in BrokerLifecycleManagerTest.scala covering the corrected behaviour.
