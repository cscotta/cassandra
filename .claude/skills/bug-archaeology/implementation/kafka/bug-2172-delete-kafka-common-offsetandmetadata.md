**Commit**: b54f0c129f · **Ticket**: KAFKA-17476
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
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
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
import org.apache.kafka.coordinator.group.{Group, OffsetAndMetadata, OffsetConfig}
import org.apache.kafka.coordinator.group.OffsetAndMetadata
import java.util.{Optional, OptionalInt, OptionalLong, Properties}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17476 delete kafka.common.offsetandmetadata (#17553).

**Test**
Test modified in GroupCoordinatorAdapterTest.scala covering the corrected behaviour.
