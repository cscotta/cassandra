**Commit**: 7d13e8b70d · **Ticket**: KAFKA-17573
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
## Changes

* Move KRaftMetadataCache from core to metadata module
* Change KRaftMetadataCache from Scala to Java

## Performance

* ReplicaFetcherThreadBenchmark
```
  ./jmh-benchmarks/jmh.sh -f 1 -i 2 -wi 2 org.apache.kafka.jmh.fetcher.ReplicaFetcherThreadBenchmark
```
* trunk
```
  Benchmark                                  (partitionCount)  Mode  Cnt
Score   Error  Units
  ReplicaFetcherThreadBenchmark.testFetcher               100  avgt    2
4669.047          ns/op
  ReplicaFetcherThreadBenchmark.testFetcher               500  avgt    2
24722.430          ns/op
  ReplicaFetcherThreadBenchmark.testFetcher              1000  avgt    2
57837.371          ns/op
  ReplicaFetcherThreadBenchma...

**Before/After**
```java
// Before
// (no removals)

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17573 move kraftmetadatacache to metadata module (#19232).

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
