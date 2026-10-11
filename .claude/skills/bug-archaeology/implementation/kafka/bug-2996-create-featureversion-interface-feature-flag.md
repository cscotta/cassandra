**Commit**: 5e3df22095 · **Ticket**: KAFKA-16308
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
As part of KIP-1022, I have created an interface for all the new features to be used when parsing the command line arguments, doing validations, getting default versions, etc. I've also added the --feature flag to the storage tool to show how it will be used. Created a TestFeatureVersion to show an implementation of the interface (besides MetadataVersion which is unique) and added tests using this new test feature. I will add the unstable config and tests in a followup.

**Before/After**
```java
// Before
 *  - they are each expected to be >= 0, as we only consider positive version values to be valid.
import org.apache.kafka.server.common.Features
  def features: Features
  val featuresProvider: () => Features
    featuresProvider: () => Features
  override def features: Features = featuresProvider.apply()
  override def features: Features = metadataCache.features()
    Features.supportedFeatures(
      java.util.Collections.singletonMap(MetadataVersion.FEATURE_NAME,
          }
        )))

// After
 *  - they are each expected to be >= 0, as we only consider non-negative version values to be valid.
import org.apache.kafka.server.common.FinalizedFeatures
  def features: FinalizedFeatures
 * @param featuresProvider a provider to the finalized features supported
  val featuresProvider: () => FinalizedFeatures
    featuresProvider: () => FinalizedFeatures
  override def features: FinalizedFeatures = featuresProvider.apply()
  override def features: FinalizedFeatures = metadataCache.features()
import org.apache.kafka.server.common.Features.PRODUCTION_FEATURES
    val features = new util.HashMap[String, SupportedVersionRange]()
      features.put(MetadataVersion.FEATURE_NAME,
          }))
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16308 [1/n]: create featureversion interface and add `--feature` flag and handling to storagetool (#15685).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
