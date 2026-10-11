**Commit**: 5bb44d63dd · **Ticket**: KAFKA-20031
**Subsystem**: transaction-coordinator/src/main/java/org/apache/kafka/coordinator/transaction/RPCProducerIdManager.java
**Category**: Off-by-one

**Root cause**
`RPCProducerIdManager.sanityCheckResponse` used a strict `<` to detect non-monotonic producer-ID blocks: it only flagged a new block if its start was strictly less than the last ID in the current block. An off-by-one meant that a new block starting at exactly `lastProducerId()` was accepted, allowing the same producer ID to be issued twice (overlap at the boundary).

**Before/After**
```java
// Before – misses equality (start == lastProducerId is still an overlap)
if (data.producerIdStart() < currentProducerIdBlock.get().lastProducerId()) {

// After – correct: start must be strictly greater than last issued ID
if (data.producerIdStart() <= currentProducerIdBlock.get().lastProducerId()) {
```

**How to spot**
Range overlap checks for monotonically-increasing sequences should use `<=` when comparing the new start against the old last value. Using `<` misses the case where the ranges share an endpoint.

**Test**
`ProducerIdManagerTest#testSanityCheckProhibitsOverlappingBlocks`: sets the current block to [100, 199], then injects a response with `producerIdStart=199` (overlapping by one) and asserts that `nextProducerIdBlock` is null (the block was rejected).
