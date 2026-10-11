**Commit**: 1146f97770 · **Ticket**: KAFKA-19800
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR is part of

[KIP-1226](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1226%3A+Introducing+Share+Partition+Lag+Persistence+and+Retrieval). This PR computes the share partition lag in GroupCoordinatorService
using `deliveryCompleteCount` received from `readSummary`, and partition
end offsets received from `adminClient.listOffstes`. The computed lag is
returned to the end user in DescribeShareGroupOffsetsResponse. NOTE: The GroupCoordinator is built with a no-op implementation of
PartitionMetadataClient, which returns -1 as the partition end offset
for any requested topic partition.

**Before/After**
```java
// Before
                                    groupOffsetsListing.put(tp, new SharePartitionOffsetInfo(startOffset, leaderEpoch, Optional.empty()));
  "validVersions": "0",
  "validVersions": "0",

// After
                                final Optional<Long> lag = partitionResponse.lag() < 0 ? Optional.empty() : Optional.of(partitionResponse.lag());
                                    groupOffsetsListing.put(tp, new SharePartitionOffsetInfo(startOffset, leaderEpoch, lag));
  // Version 0 is the initial version (KIP-932).
  //
  // Version 1 introduces Lag in the response (KIP-1226).
  "validVersions": "0-1",
  // Version 0 is the initial version (KIP-932).
  //
  // Version 1 introduces Lag (KIP-1226).
  "validVersions": "0-1",
          { "name": "Lag", "type": "int64", "versions": "1+", "ignorable": "true", "default": -1,
            "about": "The share-partition lag." },
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19800: compute share partition lag in groupcoordinatorservice (#20839).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
