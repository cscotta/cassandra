**Commit**: 43062a0eee · **Ticket**: KAFKA-19957

**Subsystem**: core/src/main/java/kafka/server/share/ShareFetchUtils.java

**Category**: Logic error in condition

**Root cause**
`SharePartition` returned one `AcquiredRecords` object per individual offset, even when consecutive offsets shared the same delivery count and belonged to the same original broker batch. This produced unnecessarily large response payloads and could cause the client to interpret single-record responses for batch-sized chunks. The fix introduces `accumulateAcquiredRecords`, which merges contiguous single-offset entries that share a delivery count into a single range record, but only within the bounds of the original broker batch to avoid crossing batch boundaries that could confuse delivery-limit accounting.

**Before**
```java
// Before: one AcquiredRecords per offset, no merging
result.add(new AcquiredRecords()
    .setFirstOffset(offset)
    .setLastOffset(offset)
    .setDeliveryCount(dc));
```

**After**
```java
// After: merge contiguous same-delivery-count offsets within batch
ShareFetchUtils.accumulateAcquiredRecords(result, acquiredRecords);
// accumulateAcquiredRecords extends lastOffset while dc == current.dc
```

**How to spot**
In `SharePartition`, look for loops that append an `AcquiredRecords` with `firstOffset == lastOffset` on every iteration; if the loop is over a sorted sequence of offsets, a merge step is almost certainly missing.

**Test**
Acquire offsets 0–9 with the same delivery count and verify that `accumulateAcquiredRecords` returns a single record spanning `[0, 9]` rather than ten individual records.
