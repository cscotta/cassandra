**Commit**: 821c10157d · **Ticket**: KAFKA-17367
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Introduces the share coordinator. This coordinator is built on the new coordinator runtime framework. It 
is responsible for persistence of share-group state in a new internal topic named "__share_group_state". The responsibility for being a share coordinator is distributed across the brokers in a cluster.

**Before/After**
```java
// Before
// (no removals)

// After
    implementation project(':share-coordinator')
    testImplementation project(':share-coordinator').sourceSets.test.output
    implementation libs.hdrHistogram
    implementation project(':server')
    implementation libs.slf4jApi
    implementation libs.metrics
                <allow pkg="org.apache.kafka.common.metrics" />
                <allow pkg="org.HdrHistogram" />
    <allow pkg="org.apache.kafka.common.compress" />
    <allow pkg="org.apache.kafka.common.metrics" />
    <allow pkg="org.apache.kafka.common.record" />
            <allow pkg="org.apache.kafka.common.network" />
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17367: introduce share coordinator  [2/n] (#17011).

**Test**
Test modified in CoordinatorRuntimeMetricsImplTest.java covering the corrected behaviour.
