**Commit**: f20f299492 · **Ticket**: KAFKA-18839
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
In 3.1 we deprecated the eager rebalancing protocol and marked it for
removal in a later release. We aim to officially drop support and remove
the protocol from Streams in 4.0. The effect of this PR is that it will no longer be possible to perform a
live upgrade Kafka Streams directly to 4.0 from version 2.3 or below. Users will have to go through a bridge release between 2.4 - 3.9
instead.

**Before/After**
```java
// Before
    private static final String UPGRADE_FROM_DOC = "Allows upgrading in a backward compatible way. " +
        "This is needed when upgrading from [0.10.0, 1.1] to 2.0+, or when upgrading from [2.0, 2.3] to 2.4+. " +
        "When upgrading from 3.3 to a newer version it is not required to specify this config. Default is `null`. " +
        "Accepted values are \"" + UPGRADE_FROM_0100 + "\", \"" + UPGRADE_FROM_0101 + "\", \"" +
        UPGRADE_FROM_0102 + "\", \"" + UPGRADE_FROM_0110 + "\", \"" + UPGRADE_FROM_10 + "\", \"" +
        UPGRADE_FROM_11 + "\", \"" + UPGRADE_FROM_20 + "\", \"" + UPGRADE_FROM_21 + "\", \"" +
        UPGRADE_FROM_22 + "\", \"" + UPGRADE_FROM_23 + "\", \"" + UPGRADE_FROM_24 + "\", \"" +
    private RebalanceProtocol rebalanceProtocol;
        usedSubscriptionMetadataVersion = assignorConfiguration.configuredMetadataVersion(usedSubscriptionMetadataVersion);
        rebalanceProtocol = assignorConfiguration.rebalanceProtocol();
        final List<RebalanceProtocol> supportedProtocols = new ArrayList<>();
        supportedProtocols.add(R

// After
    private static final String UPGRADE_FROM_DOC = "Allows live upgrading (and downgrading in some cases -- see upgrade guide) in a backward compatible way. Default is `null`. " +
        "Please refer to the Kafka Streams upgrade guide for instructions on how and when to use this config. " +
        "Note that when upgrading from 3.5 to a newer version it is never required to specify this config, " +
        "while upgrading live directly to 4.0+ from 2.3 or below is no longer supported even with this config. " +
        "Accepted values are \"" + UPGRADE_FROM_24 + "\", \"" +
import static java.util.Collections.singletonList;
    // since live upgrades from 2.3 (or earlier) to 4.0 or above are no longer supported, we can always
    // start with the latest supported metadata version since version probing will take
    // care of downgrading it if/when necessary
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18839: drop eager rebalancing support in kafka streams (#18988).

**Test**
Test modified in StreamsPartitionAssignorTest.java covering the corrected behaviour.
