**Commit**: 06185618b1 · **Ticket**: KAFKA-19817
**Subsystem**: checkstyle/import-control-metadata.xml
**Category**: Logic error in condition

**Root cause**
see: [KAFKA-19817](https://issues.apache.org/jira/browse/KAFKA-19817)
This PR intended to move `DynamicTopicClusterQuotaPublisher`  to the
metadata module . To prevent circular dependencies, several associated
classes have been migrated as well，including `ClientSensors` ,
`ClientQuotaMetadataManager`, `ThrottleCallback` and `ThrottledChannel`. Additionally, a new interface `QuotaManagersProvider` has been
introduced to avoid the need to move class `QuotaManagers` in
`QuotaFactory`, as doing so would have necessitated significantly more
extensive changes. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.network.Session
import org.apache.kafka.network.RequestConvertToJson
import org.apache.kafka.metadata.publisher.{AclPublisher, DelegationTokenPublisher, DynamicClientQuotaPublisher, ScramPublisher}
          config,
          quotaManagers,
import kafka.server.metadata.{ClientQuotaMetadataManager, DynamicConfigPublisher, DynamicTopicClusterQuotaPublisher, KRaftMetadataCach

// After
        <allow pkg="org.apache.kafka.server.quota" />
import org.apache.kafka.metadata.publisher.QuotaConfigChangeListener;

        public QuotaConfigChangeListener quotaConfigChangeListener() {
            return () -> {
                fetch.updateQuotaMetricConfigs();
                produce.updateQuotaMetricConfigs();
                request.updateQuotaMetricConfigs();
                controllerMutation.updateQuotaMetricConfigs();
            };
        }
import org.apache.kafka.network.{RequestConvertToJson, Session}
```

**How to spot**
When reviewing checkstyle/import-control-metadata.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19817 move dynamictopicclusterquotapublisher to metadata module (#20783).

**Test**
Test modified in ClientQuotaManagerTest.scala covering the corrected behaviour.
