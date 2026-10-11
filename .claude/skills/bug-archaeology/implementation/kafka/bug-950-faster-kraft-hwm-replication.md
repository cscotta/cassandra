**Commit**: 742b327025 · **Ticket**: KAFKA-14145
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This change compares the remote replica's HWM with the leader's HWM and
completes the FETCH request if the remote HWM is less than the leader's
HWM. When the leader's HWM is updated any pending FETCH RPC is
completed. Reviewers: Alyssa Huang <ahuang@confluent.io>, David Arthur
 <mumrah@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
  "validVersions": "4-17",
          "about": "The directory id of the follower fetching." }
  "validVersions": "4-17",
        assertEquals(ControllerResult.of(List.of(), new ApiError(Errors.INVALID_UPDATE_VERSION,
            "Invalid update version 6 for feature metadata.version. Local controller 0 only supports versions 7-28")),
                manager.updateFeatures(
                        Map.of(MetadataVersion.FEATURE_NAME, MetadataVersionTestUtils.IBP_3_3_IV2_FEATURE_LEVEL),
                        Map.of(MetadataVersion.FEATURE_NAME, FeatureUpdate.UpgradeType.UNSAFE_DOWNGRADE),
                        true,
                        0));
import org.apache.kafka.common.errors.NotLeaderOrFollowerException;

// After
  //
  // Version 18 adds high-watermark from KIP-1166
  "validVersions": "4-18",
          "about": "The directory id of the follower fetching." },
        { "name": "HighWatermark", "type": "int64", "versions": "18+", "default": "9223372036854775807", "taggedVersions": "18+",
          "tag": 1, "ignorable": true,
          "about": "The high-watermark known by the replica. -1 if the high-watermark is not known and 9223372036854775807 if the feature is not supported." }
  //
  // Version 18 no changes to the response (KIP-1166)
  "validVersions": "4-18",
        assertEquals(
            ControllerResult.of(
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14145; faster kraft hwm replication (#19800).

**Test**
Test modified in FeatureControlManagerTest.java covering the corrected behaviour.
