**Commit**: da46cf6e79 · **Ticket**: KAFKA-17565
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Logic error in condition

**Root cause**
### Changes

* Move MetadataCache interface to metadata module and change Scala
function to Java. * Remove functions `getTopicPartitions`, `getAliveBrokers`,
`topicNamesToIds`, `topicIdInfo`, and `getClusterMetadata` from
MetadataCache interface, because these functions are only used in test
code. ### Performance

* ReplicaFetcherThreadBenchmark
  ```
./jmh-benchmarks/jmh.sh -f 1 -i 2 -wi 2
org.apache.kafka.jmh.fetcher.ReplicaFetcherThreadBenchmark
  ```
  * trunk
  ```
Benchmark (partitionCount) Mode Cnt Score Error Units
ReplicaFetcherThreadBenchmark.testFetcher 100 avgt 2 4775.490 ns/op
ReplicaFetcherThreadBenchmark.testFetcher 500 avgt 2 25730.790 ns/op
ReplicaFetcherThreadBenchmark.test...

**Before/After**
```java
// Before
// (no removals)

// After
// (no additions)
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17565 move metadatacache interface to metadata module (#18801).

**Test**
Test modified in RackAwareAutoTopicCreationTest.java covering the corrected behaviour.
