**Commit**: 1af5faef73 · **Ticket**: KAFKA-20130
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Moved to storage and turned into a record for immutability. Removed
`add()` because it didn't seem to be used. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
import org.apache.kafka.storage.internals.log.{AppendOrigin, RecordValidationStats}
import org.apache.kafka.storage.internals.log.{AppendOrigin, FetchDataInfo, FetchPartitionStatus, LeaderHwChange, LogAppendInfo, LogConfig, LogDirFailureChannel, LogOffsetMetadata, LogReadInfo, LogReadResult, OffsetResultHolder, RecordValidationException, RecordValidationStats, RemoteLogReadResult,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20130 move recordvalidationstats to storage module (#21418).

**Test**
Test modified in AbstractCoordinatorConcurrencyTest.scala covering the corrected behaviour.
