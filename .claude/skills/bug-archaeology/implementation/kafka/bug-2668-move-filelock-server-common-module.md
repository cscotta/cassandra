**Commit**: 437b86cd20 · **Ticket**: KAFKA-17176
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.util.Scheduler
import kafka.utils.FileLock
import org.apache.kafka.server.util.KafkaScheduler
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0

// After
    <allow pkg="org.apache.kafka.server.util" />
import org.apache.kafka.server.util.{FileLock, Scheduler}
import org.apache.kafka.server.util.{FileLock, KafkaScheduler}
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17176 move filelock to server-common module (#16671).

**Test**
Test modified in LogManagerTest.scala covering the corrected behaviour.
