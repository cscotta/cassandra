**Commit**: e30edb3eff · **Ticket**: KAFKA-18052
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Currently the validation of feature upgrade relies on the supported version range generated during registration. For a given feature, its max supported feature version in production is set to be the default version value (the latest feature version with bootstrap metadata value smaller or equal to the latest production metadata value). This patch introduces a LATEST_PRODUCTION value independent from the metadata version to each feature so that the highest supported feature version can be customized by the feature owner. The change only applies to dynamic feature upgrade.

**Before/After**
```java
// Before
import org.apache.kafka.server.common.Features
      Features.KRAFT_VERSION.supportedVersionRange(),
import org.apache.kafka.server.common.{Features, MetadataVersion}
        runVersionMappingCommand(namespace, printStream, Features.PRODUCTION_FEATURES)
        runFeatureDependenciesCommand(namespace, printStream, Features.PRODUCTION_FEATURES)
    validFeatures: java.util.List[Features]
        val featureLevel = feature.defaultValue(metadataVersion)
    validFeatures: java.util.List[Features]
import org.apache.kafka.server.common.{ControllerRequestCompletionHandler, Features, MetadataVersion, NodeToControllerChannelManager}
    Features.PRODUCTION_FEATURES.stream().filter(_.featureName != MetadataVersion.

// After
import org.apache.kafka.server.common.Feature
      Feature.KRAFT_VERSION.supportedVersionRange(),
import org.apache.kafka.server.common.{Feature, MetadataVersion}
        runVersionMappingCommand(namespace, printStream, Feature.PRODUCTION_FEATURES)
        runFeatureDependenciesCommand(namespace, printStream, Feature.PRODUCTION_FEATURES)
    validFeatures: java.util.List[Feature]
        val featureLevel = feature.defaultLevel(metadataVersion)
    validFeatures: java.util.List[Feature]
import org.apache.kafka.server.common.{ControllerRequestCompletionHandler, Feature, MetadataVersion, NodeToControllerChannelManager}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18052: decouple the dependency of feature stable version to the metadata version (#17886).

**Test**
Test modified in BrokerRegistrationRequestTest.scala covering the corrected behaviour.
