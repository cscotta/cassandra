**Commit**: ebae768bd8 · **Ticket**: KAFKA-18193
**Subsystem**: docs/upgrade.html
**Category**: Resource leak

**Root cause**
In Kafka Streams, configuration classes typically follow a fluent API
pattern to ensure a consistent and intuitive developer experience. However, the current implementation of
`org.apache.kafka.streams.KafkaStreams$CloseOptions` deviates from this
convention by exposing a public constructor, breaking the uniformity
expected across the API. To address this inconsistency, we propose introducing a new
`CloseOptions` class that adheres to the fluent API style, replacing the
existing implementation. The new class will retain the existing
`timeout(Duration)` and `leaveGroup(boolean)` methods but will enforce
fluent instantiation and configuration.

**Before/After**
```java
// Before
import org.apache.kafka.streams.KafkaStreams.CloseOptions;
        streams.close(new CloseOptions().leaveGroup(true).timeout(Duration.ofSeconds(30)));

// After
    <li>
        Deprecated <code>org.apache.kafka.streams.KafkaStreams$CloseOptions</code> and its related methods, such as
        <code>KafkaStreams#close(org.apache.kafka.streams.KafkaStreams$CloseOptions)</code>.
        As a replacement, please use <code>org.apache.kafka.streams.CloseOptions</code> and
        <code>KafkaStreams#close(org.apache.kafka.streams.CloseOptions)</code>.
        For further details, please refer to <a href="https://cwiki.apache.org/confluence/x/QAq9F">KIP-1153</a>.
    </li>
import org.apache.kafka.streams.CloseOptions;
        streams.close(CloseOptions.groupMembershipOperation(CloseOptions.GroupMembershipOperation.LEAVE_GROUP).withTimeout(Duration.ofSeconds(30)));
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
```

**How to spot**
When reviewing docs/upgrade.html code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18193 refactor kafka streams closeoptions to fluent api style (#19955).

**Test**
Test modified in KafkaStreamsCloseOptionsIntegrationTest.java covering the corrected behaviour.
