**Commit**: 343bc995f4 · **Ticket**: KAFKA-18920
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The kafka controllers need to set kraft.version in their
ApiVersionsResponse messages according to the current kraft.version
reported by the Raft layer. Instead, currently they always set it to 0. Also remove FeatureControlManager.latestFinalizedFeatures. It is not
needed and it does a lot of copying.

**Before/After**
```java
// Before
import org.apache.kafka.server.common.{ApiMessageAndVersion, NodeToControllerChannelManager}
        () => featuresPublisher.features()
    FinalizedControllerFeatures latestFinalizedFeatures() {
        Map<String, Short> features = new HashMap<>();
        features.put(MetadataVersion.FEATURE_NAME, metadataVersion.get().featureLevel());
        for (Entry<String, Short> entry : finalizedVersions.entrySet()) {
            features.put(entry.getKey(), entry.getValue());
        }
        return new FinalizedControllerFeatures(features, -1);
    }

        return latestFinalizedFeatures().versionOrDefault(EligibleLeaderReplicasVersion.FEATURE_NAME, (short) 0) >=

// After
import org.apache.kafka.server.common.{ApiMessageAndVersion, KRaftVersion, NodeToControllerChannelManager}
        () => featuresPublisher.features().setFinalizedLevel(
          KRaftVersion.FEATURE_NAME,
          raftManager.client.kraftVersion().featureLevel())
  @ParameterizedTest
  @ValueSource(booleans = Array(false, true))
  def testDescribeKRaftVersion(usingBootstrapControlers: Boolean): Unit = {
    val cluster = new KafkaClusterTestKit.Builder(
      new TestKitNodes.Builder().
        setNumBrokerNodes(1).
        setNumControllerNodes(1).
        setFeature(KRaftVersion.FEATURE_NAME, 1.toShort).build()).build()
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18920: the kcontrollers must set kraft.version in apiversionsresponse (#19127).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
