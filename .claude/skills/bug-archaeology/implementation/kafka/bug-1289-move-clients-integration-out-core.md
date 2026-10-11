**Commit**: fcca4056fd · **Ticket**: KAFKA-18975
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Move following tests from core to clients-integration-test module. - ClientTelemetryTest
- DeleteTopicTest
- DescribeAuthorizedOperationsTest
- ConsumerIntegrationTest
- CustomQuotaCallbackTest
- RackAwareAutoTopicCreationTest

Move following tests from core to server module. - BootstrapControllersIntegrationTest
- LogManagerIntegrationTest

Reviewers: Kirk True <kirk@kirktrue.pro>, Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    implementation project(':clients').sourceSets.test.output
package kafka.admin;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
                @ClusterConfigProperty(key = METRIC_REPORTER_CLASSES_CONFIG, value = "kafka.admin.ClientTelemetryTest$GetIdClientTelemetry"),
     * {@link  org.apache.kafka.common.protocol.ApiKeys.GET_TELEMETRY_SUBSCRIPTIONS } command will not be supported
package kafka.admin;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.clients.admin.NewPartitions;

// After
    testImplementation project(':test-common:test-common-internal-api')
    testImplementation project(':test-common:test-common-runtime')
    testImplementation project(':metadata')
    testImplementation project(':server')
    testImplementation project(':storage')
    testImplementation project(':core').sourceSets.test.output
    testImplementation project(':clients').sourceSets.test.output
    <allow pkg="org.apache.kafka.storage.internals.checkpoint" />
package org.apache.kafka.clients.admin;

import kafka.admin.ConfigCommand;
                @ClusterConfigProperty(key = METRIC_REPORTER_CLASSES_CONFIG, value = "org.apache.kafka.clients.admin.ClientTelemetryTest$GetIdClientTelemetry"),
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18975 move clients-integration-test out of core module (#19217).

**Test**
Test modified in ClientTelemetryTest.java covering the corrected behaviour.
