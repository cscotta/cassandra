**Commit**: bf91c07993 · **Ticket**: KAFKA-15599
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
- Rename `KafkaMetadataLog` to `KafkaRaftLog`
- Make `raft` depend on `storage`
- Remove `storage` dependency on `metadata` as this otherwise create a
cyclic dependency

Reviewers: Ismael Juma <mail@ismaeljuma.com>, Jun Rao <junrao@gmail.com>, José Armando García Sancio <jsancio@apache.org>, Ziming Deng <dengziming1993@gmail.com>

**Before/After**
```java
// Before
    implementation project(':metadata')
        <allow pkg="org.apache.kafka.image" />
        <allow pkg="org.apache.kafka.metadata" />

// After
    testImplementation project(':raft')
    implementation project(':storage')
    testImplementation project(':raft')
    testImplementation project(':raft')
    <subpackage name="cache">
    </subpackage>
    <subpackage name="metadata">
        <allow class="org.apache.kafka.raft.KRaftConfigs"/>
    </subpackage>
    <allow pkg="org.apache.kafka.common.internals" />
    <allow pkg="org.apache.kafka.server.storage.log" />
    <allow pkg="org.apache.kafka.storage.internals.log" />
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15599: move kafkametadatalog to the raft module (#19762).

**Test**
Test modified in StaticBrokerConfigTest.java covering the corrected behaviour.
