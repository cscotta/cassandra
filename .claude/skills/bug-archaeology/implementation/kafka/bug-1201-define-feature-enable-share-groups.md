**Commit**: 21a080f08c · **Ticket**: KAFKA-16894
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
This PR proposes a switch to enable share groups for 4.1 (preview) and
4.2 (GA). * `share.version=1` to indicate that share groups are enabled. This is
used as the switch for turning share groups on and off. In 4.1, the default will be `share.version=0`.

**Before/After**
```java
// Before
import org.apache.kafka.server.common.{EligibleLeaderReplicasVersion, GroupVersion, MetadataVersion, TransactionVersion}
      assertEquals(4, apiVersionsResponse.data().finalizedFeatures().size())
      assertEquals(5, apiVersionsResponse.data().supportedFeatures().size())
      "eligible.leader.replicas.version, group.version, kraft.version, transaction.version",
            "Invalid update version 6 for feature metadata.version. Local controller 0 only supports versions 7-26")),

// After
import org.apache.kafka.server.common.{EligibleLeaderReplicasVersion, GroupVersion, MetadataVersion, ShareVersion, TransactionVersion}
      assertEquals(5, apiVersionsResponse.data().finalizedFeatures().size())
      assertEquals(6, apiVersionsResponse.data().supportedFeatures().size())

      assertEquals(0, apiVersionsResponse.data().supportedFeatures().find(ShareVersion.FEATURE_NAME).minVersion())
      assertEquals(ShareVersion.SV_1.featureLevel(), apiVersionsResponse.data().supportedFeatures().find(ShareVersion.FEATURE_NAME).maxVersion())
      "eligible.leader.replicas.version, group.version, kraft.version, share.version, transaction.version",
            "Invalid update version 6 for feature metadata.version. Local controller 0 only supports versions 7-28")),
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16894: define feature to enable share groups (#19293).

**Test**
Test modified in AbstractApiVersionsRequestTest.scala covering the corrected behaviour.
