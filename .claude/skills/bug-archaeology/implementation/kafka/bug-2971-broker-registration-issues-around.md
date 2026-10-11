**Commit**: 8ace33b47f · **Ticket**: KAFKA-16757
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When upgrading from a MetadataVersion older than 3.7-IV2, we need to resend the broker registration, so that the controller can record the storage directories. The current code for doing this has several problems, however. One is that it tends to trigger even in cases where we don't actually need it. Another is that when re-registering the broker, the broker is marked as fenced.

**Before/After**
```java
// Before
  def handleKraftJBODMetadataVersionUpdate(): Unit = {
    eventQueue.append(new KraftJBODMetadataVersionUpdateEvent())
  private class KraftJBODMetadataVersionUpdateEvent extends EventQueue.Event {
import org.apache.kafka.image.publisher.MetadataPublisher
import org.apache.kafka.server.common.MetadataVersion
      Option(delta.featuresDelta()).foreach { featuresDelta =>
        featuresDelta.metadataVersionChange().ifPresent{ metadataVersion =>
          info(s"Updating metadata.version to ${metadataVersion.featureLevel()} at offset $highestOffsetAndEpoch.")
          val currentMetadataVersion = delta.image().features().metadataVersion()
          if (currentMetadataVersion.isLessThan(MetadataVersion.IBP_3_7_IV2) && metadataVersion.isAtLeast(MetadataVersion.IBP_3_7_IV2)) {
            info(
              s"""Resending BrokerRegistration with existing incarnation-id to inform the

// After
  def resendBrokerRegistrationUnlessZkMode(): Unit = {
    eventQueue.append(new ResendBrokerRegistrationUnlessZkModeEvent())
  private class ResendBrokerRegistrationUnlessZkModeEvent extends EventQueue.Event {
import org.apache.kafka.image.publisher.{BrokerRegistrationTracker, MetadataPublisher}
  var brokerRegistrationTracker: BrokerRegistrationTracker = _

      brokerRegistrationTracker = new BrokerRegistrationTracker(config.brokerId,
        logManager.directoryIdsSet.toList.asJava,
        () => lifecycleManager.resendBrokerRegistrationUnlessZkMode())
      metadataPublishers.add(brokerRegistrationTracker)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16757: fix broker re-registration issues around mv 3.7-iv2 (#15945).

**Test**
Test modified in BrokerLifecycleManagerTest.scala covering the corrected behaviour.
