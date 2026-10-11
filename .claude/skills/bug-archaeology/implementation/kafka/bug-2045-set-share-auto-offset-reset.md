**Commit**: 6cf4081540 · **Ticket**: KAFKA-17985
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
After the share.auto.offset.reset dynamic config was added for share groups in this commit - 9db5ed0, we needed to update this config value to "earliest" in ShareRoundTripWorker when it creates the consumer. Reviewers: Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    private static Admin createAdminClient(


        props.put(ConsumerConfig.GROUP_ID_CONFIG, "round-trip-share-group-" + id);

// After
    implementation project(':group-coordinator')
    implementation project(':group-coordinator:group-coordinator-api')

    testImplementation project(':group-coordinator')

    <allow pkg="org.apache.kafka.coordinator" />
    public static Admin createAdminClient(
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsOptions;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17985: set share.auto.offset.reset to earliest in shareroundtripworker (#17758).

**Test**
No test added.
