**Commit**: 863b728c40 · **Ticket**: KAFKA-17677
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Since we are removing support for Scala 2.12, CoreUtils#atomicGetOrUpdate should also be removed, as it is a workaround for the different behavior between ConcurrentMap.getOrElse and JConcurrentMapWrapper. JMH results show no significant performance regression when using `getOrElseUpdate`:

Benchmark                                                            (mapSize)  (writePercentage)  Mode  Cnt  Score   Error  Units
ConcurrentMapBenchmark.testConcurrentHashMapComputeIfAbsentReadOnly        100                0.1  avgt    5  4.182 ± 0.006  ns/op
ConcurrentMapBenchmark.testConcurrentHashMapGetReadOnly                    100                0.1  avgt    5  3.372 ± 0.024  ns/op
JMH benchmarks ...

**Before/After**
```java
// Before
import kafka.utils.{CoreUtils, Logging}
    val queue = CoreUtils.atomicGetOrUpdate(markersPerTxnTopicPartition, txnTopicPartition, {
    val brokerRequestQueue = CoreUtils.atomicGetOrUpdate(markersQueuePerBroker, brokerId, {
  /**
   * Atomic `getOrElseUpdate` for concurrent maps. This is optimized for the case where
   * keys often exist in the map, avoiding the need to create a new value. `createValue`
   * may be invoked more than once if multiple threads attempt to insert a key at the same
   * time, but the same inserted value will be returned to all threads.
   *
   * In Scala 2.12, `ConcurrentMap.getOrElse` has the same behaviour as this method, but JConcurrentMapWrapper that
   * wraps Java maps does not.
   */

// After
import kafka.utils.Logging
    val queue = markersPerTxnTopicPartition.getOrElseUpdate(txnTopicPartition, {
    val brokerRequestQueue = markersQueuePerBroker.getOrElseUpdate(brokerId, {
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17677 remove atomicgetorupdate (#17484).

**Test**
Test modified in CoreUtilsTest.scala covering the corrected behaviour.
