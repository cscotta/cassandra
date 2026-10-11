**Commit**: 4c8d96c0f0 · **Ticket**: KAFKA-18767
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        this(new ConsumerConfig(ConsumerConfig.appendDeserializerToConfig(configs, keyDeserializer, valueDeserializer)),
    KafkaShareConsumer(ConsumerConfig config,
                       final ConsumerConfig config,


// After
        this(new ShareConsumerConfig(ShareConsumerConfig.appendDeserializerToConfig(configs, keyDeserializer, valueDeserializer)),
    KafkaShareConsumer(ShareConsumerConfig config,
                       final ShareConsumerConfig config,
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18767: add client side config check for shareconsumer (#18850).

**Test**
Test modified in KafkaShareConsumerMetricsTest.java covering the corrected behaviour.
