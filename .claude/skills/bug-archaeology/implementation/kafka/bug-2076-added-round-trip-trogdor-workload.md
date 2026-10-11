**Commit**: 0181073d49 · **Ticket**: KAFKA-17933
**Subsystem**: trogdor/src
**Category**: Logic error in condition

**Root cause**
Added ShareRoundTripWorker.java similar to RoundTripWorker.java. This will start a producer and a share consumer on a single node. The share consumer reads back the messages produced by the producer. Reviewers: Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    protected Map<String, String> configOrEmptyMap(Map<String, String> config) {

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.apache.kafka.common.serialization.ByteArraySerializer;

// After
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
```

**How to spot**
When reviewing trogdor/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17933: added round trip trogdor workload for share consumer. (#17692).

**Test**
Test modified in share_round_trip.json covering the corrected behaviour.
