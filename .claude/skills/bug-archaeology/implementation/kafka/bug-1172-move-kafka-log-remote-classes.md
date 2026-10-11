**Commit**: c73d97de0c · **Ticket**: KAFKA-14523
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Pretty much a straight forward move of these classes. I just updated
`RemoteLogManagerTest` to not use `KafkaConfig`

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  <subpackage name="log.remote">
    <allow pkg="org.apache.kafka.server.common" />
    <allow pkg="org.apache.kafka.server.log.remote" />
    <allow pkg="org.apache.kafka.server.log.remote.quota" />
    <allow pkg="org.apache.kafka.server.metrics" />
    <allow pkg="org.apache.kafka.storage.internals" />
    <allow pkg="org.apache.kafka.storage.log.metrics" />
    <allow pkg="kafka.log" />
    <allow pkg="kafka.cluster" />
    <allow pkg="kafka.server" />
    <allow pkg="org.mockito" />
    <allow pkg="org.apache.kafka.test" />

// After
    testImplementation project(':storage:storage-api').sourceSets.test.output
    <allow pkg="org.opentest4j" />
                    <allow pkg="org.apache.kafka.server.metrics" />
                    <allow pkg="org.apache.kafka.server.purgatory" />
                    <allow pkg="org.apache.kafka.server.quota" />
                    <allow pkg="org.apache.kafka.server.storage.log" />
                    <allow pkg="org.apache.kafka.server.util" />
              files="(SharePartitionManagerTest|SharePartitionTest).java"/>
              files="(UnifiedLog|RemoteLogManager|RemoteLogManagerTest).java"/>
    <suppress checks="MethodLength" files="(RemoteLogManager|RemoteLogManagerConfig).java"/>
    <suppress checks="JavaNCSS" files="RemoteLogManagerTest.java"/>
import org.apache.kafka.server.log.remote.storage.RemoteLogManager;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14523: move kafka.log.remote classes to storage (#19474).

**Test**
Test modified in LogTestUtils.scala covering the corrected behaviour.
