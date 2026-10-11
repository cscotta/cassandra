**Commit**: 200f3896bc · **Ticket**: KAFKA-15894
**Subsystem**: core/src/main/scala/kafka/server/KafkaApis.scala
**Category**: Logic error in condition

**Root cause**
`KafkaApis.updateRecordConversionStats` used assignment (`=`) rather than accumulation (`+=`) when recording `messageConversionsTimeNanos` and `temporaryMemoryBytes`. The `processingStatsCallback` is invoked once per partition in a produce request; with simple assignment, each invocation overwrites the previous value so only the last partition's conversion stats survive, making the metrics arbitrarily under-reported for multi-partition produce requests.

**Before/After**
```scala
// Before
request.messageConversionsTimeNanos = conversionStats.conversionTimeNanos
request.temporaryMemoryBytes       = conversionStats.temporaryMemoryBytes

// After
request.messageConversionsTimeNanos += conversionStats.conversionTimeNanos
request.temporaryMemoryBytes       += conversionStats.temporaryMemoryBytes
```

**How to spot**
Metrics that should be aggregates across partitions but are set inside a per-partition callback are prone to this. Search for assignment inside any callback that is called N times per request; if the intent is accumulation, `+=` is required.

**Test**
`KafkaApisTest#testRecordConversionStatsAccumulatedAcrossMultiplePartitions`: issues a two-partition produce request with `RecordValidationStats(1000L,5,100L)` for partition 0 and `(2000L,3,200L)` for partition 1, then asserts `messageConversionsTimeNanos == 300` and `temporaryMemoryBytes == 3000`.
