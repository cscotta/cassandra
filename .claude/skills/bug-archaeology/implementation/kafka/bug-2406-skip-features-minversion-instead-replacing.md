**Commit**: e311716beb · **Ticket**: KAFKA-17492
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The 3.8 controller assumes the unknown features have min version = 0, but KAFKA-17011 replace the min=0 by min=1 when BrokerRegistrationRequest < 4. Hence, to support upgrading from 3.8.0 to 3.9, this PR changes the implementation of ApiVersionsResponse (<4) and BrokerRegistrationRequest (<4) to skip features with supported minVersion of 0 instead of replacing 0 with 1

Reviewers: Jun Rao <junrao@gmail.com>, Colin P. McCabe <cmccabe@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            final SupportedFeatureKey key = new SupportedFeatureKey();
            key.setName(feature.getKey());
                // at versions less than 4, we must set the minimum version for these features
                // to 1 rather than 0. See KAFKA-17011 for details.
                key.setMinVersion((short) 1);
            key.setMaxVersion(versionRange.max());
            converted.add(key);
import java.util.Iterator;
                // Workaround for KAFKA-17011: for BrokerRegistrationRequest versions older than 4,
                // translate minSupportedVersion = 0 to minSupportedVersion = 1.
                for (Iterator<BrokerRegistrationRequestData.Feature> iter = newData.features().iterator();
                     iter.hasNext(); ) {

// After
                // at versions less than 4, we must omit these features. See KAFKA-17492.
                final SupportedFeatureKey key = new SupportedFeatureKey();
                key.setName(feature.getKey());
                key.setMaxVersion(versionRange.max());
                converted.add(key);
                // Workaround for KAFKA-17492: for BrokerRegistrationRequest versions older than 4,
                // remove features with minSupportedVersion = 0.
                newData.features().removeIf(feature -> feature.minSupportedVersion() == 0);
      "about": "Features supported by the broker. Note: in v0-v3, features with MinSupportedVersion = 0 are omitted.",
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17492 skip features with minversion of 0 instead of replacing 0 with 1 when brokerregistrationrequest < 4 (#17128).

**Test**
Test modified in ApiVersionsResponseTest.java covering the corrected behaviour.
