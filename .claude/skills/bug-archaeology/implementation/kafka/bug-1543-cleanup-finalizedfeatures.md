**Commit**: fa2df3bca7 · **Ticket**: KAFKA-18559
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      image.highestOffsetAndEpoch().offset,
      true)
        0,
        true
      )
      0,
      true
    )
        0,
        true
      )
      () => new FinalizedFeatures(MetadataVersion.latestTesting(), Collections.emptyMap[String, java.lang.Short], 0, true))

// After
      image.highestOffsetAndEpoch().offset)
        0)
      0)
        0)
      () => new FinalizedFeatures(MetadataVersion.latestTesting(), Collections.emptyMap[String, java.lang.Short], 0))
      () => new FinalizedFeatures(MetadataVersion.latestTesting(), Collections.emptyMap[String, java.lang.Short], 0))
      () => new FinalizedFeatures(MetadataVersion.latestTesting(), Collections.emptyMap[String, java.lang.Short], 0))
    () => new FinalizedFeatures
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18559 cleanup finalizedfeatures (#18593).

**Test**
Test modified in TransactionCoordinatorConcurrencyTest.scala covering the corrected behaviour.
