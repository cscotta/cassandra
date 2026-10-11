**Commit**: 08a93fe12a · **Ticket**: KAFKA-14523
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
Decouple RemoteLogManager and ReplicaManager. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.DelayedRemoteListOffsets;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software

// After
        <allow pkg="com.yammer.metrics.core" />
        <allow pkg="org.apache.kafka.server.metrics" />
        <allow pkg="org.apache.kafka.server.util.timer" />
        <allow pkg="org.apache.kafka.storage.internals.log" />
import org.apache.kafka.server.purgatory.DelayedRemoteListOffsets;
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14523: move delayedremotelistoffsets to the storage module (#19285).

**Test**
Test modified in DelayedRemoteListOffsetsTest.scala covering the corrected behaviour.
