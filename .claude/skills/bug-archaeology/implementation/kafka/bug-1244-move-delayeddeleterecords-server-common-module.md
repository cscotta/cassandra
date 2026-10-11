**Commit**: 2994e5eff3 · **Ticket**: KAFKA-19004
**Subsystem**: checkstyle/import-control-server-common.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ken Huang
<s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.purgatory.{DelayedOperationPurgatory, TopicPartitionOperationKey}
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software

// After
            <allow pkg="com.yammer.metrics.core" />
import org.apache.kafka.server.purgatory.{DelayedDeleteRecords, DelayedOperationPurgatory, TopicPartitionOperationKey}
```

**How to spot**
When reviewing checkstyle/import-control-server-common.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19004 move delayeddeleterecords to server-common module (#19226).

**Test**
Test modified in PartitionTest.scala covering the corrected behaviour.
