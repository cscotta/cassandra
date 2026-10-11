**Commit**: b805877705 · **Ticket**: KAFKA-18969
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Move share consumer to clients-integration-tests module and use `@BeforeEach` to setup

Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
package kafka.test.api;
import kafka.api.BaseConsumerTest;
import org.apache.kafka.clients.consumer.AcknowledgeType;
import org.apache.kafka.clients.consumer.AcknowledgementCommitCallback;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.ShareConsumer;

    private void setup() {

// After
    testImplementation project(':metadata')
    implementation project(':clients').sourceSets.test.output
  <allow pkg="org.junit" />
package org.apache.kafka.clients.consumer;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.BeforeEach;
    private static final String KEY = "content-type";
    private static final String VALUE = "application/octet-stream";

    @BeforeEach
    public void setup() {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18969 rewrite shareconsumertest#setup and move to clients-integration-tests module (#19202).

**Test**
Test modified in import-control-clients-integration-tests.xml covering the corrected behaviour.
