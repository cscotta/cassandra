**Commit**: 29d0ec207e · **Ticket**: KAFKA-14487
**Subsystem**: checkstyle/import-control-server-common.xml
**Category**: Logic error in condition

**Root cause**
I kept the Scala factory (`LogManager` object) for now as it uses
`KafkaConfig` but this is migrating all the logic to Java. I tried to
modify `LogMangerTest` as little as possible. I've run the 4 JMH benchmarks this touches and saw no performance
changes:

```
PR:
Benchmark                                  (partitionCount)  Mode  Cnt
Score       Error  Units
ReplicaFetcherThreadBenchmark.testFetcher               100  avgt    3
4929.594 ±   527.256  ns/op
ReplicaFetcherThreadBenchmark.testFetcher               500  avgt    3
28161.077 ±  1448.314  ns/op
ReplicaFetcherThreadBenchmark.testFetcher              1000  avgt    3
49003.192 ±  2650.147  ns/op
ReplicaFetcherThreadBenchmark.testFetch...

**Before/After**
```java
// Before
        <allow pkg="org.apache.kafka.server.common" />

// After
        <allow pkg="org.apache.kafka.common.config" />
        <allow pkg="org.apache.kafka.metadata.properties" />
        <allow pkg="org.apache.kafka.server.common" />
        <allow pkg="org.apache.kafka.test" />
        <allow pkg="org.apache.kafka.metadata" />
        <allow pkg="org.apache.kafka.metadata.properties" />
```

**How to spot**
When reviewing checkstyle/import-control-server-common.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14487 move logmanager to storage module (#21834).

**Test**
Test modified in DeleteTopicTest.java covering the corrected behaviour.
