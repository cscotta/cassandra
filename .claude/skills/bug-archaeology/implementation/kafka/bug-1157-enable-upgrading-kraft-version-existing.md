**Commit**: b97a130c08 · **Ticket**: KAFKA-16538
**Subsystem**: metadata/src
**Category**: Resource leak

**Root cause**
This change implements upgrading the kraft version from 0 to 1 in existing clusters. Previously, clusters were formatted with either version 0 or version 1, and could not
be moved between them. The kraft version for the cluster metadata partition is recorded using the
KRaftVersion control record. If there is no KRaftVersion control record
the default kraft version is 0.

**Before/After**
```java
// Before
        boolean validateOnly
        ControllerResult<ApiError> result = featureControl.updateFeatures(updates, upgradeTypes, validateOnly);

// After
     * @param currentClaimEpoch the currently claimed epoch
        boolean validateOnly,
        int currentClaimEpoch
        ControllerResult<ApiError> result = featureControl.updateFeatures(updates, upgradeTypes, validateOnly, currentClaimEpoch);
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.metadata.NoOpRecord;
        private KRaftVersionAccessor kraftVersionAccessor = null;

        Builder setKRaftVersionAccessor(KRaftVersionAccessor kraftVersionAccessor) {
            this.kraftVersionAccessor = kraftVersionAccessor;
            return this;
        }
```

**How to spot**
When reviewing metadata/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16538; enable upgrading kraft version for existing clusters (#19416).

**Test**
Test modified in ConfigurationControlManagerTest.java covering the corrected behaviour.
