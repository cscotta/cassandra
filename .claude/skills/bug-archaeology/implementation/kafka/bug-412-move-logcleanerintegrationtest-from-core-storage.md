**Commit**: 12eca09fb4 · **Ticket**: KAFKA-20178
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR is a follow-up PR from
https://github.com/apache/kafka/pull/21406. The last one should be
`LogCleanerParameterizedIntegrationTest` and then we can simply remove
Abstract scala class also. I have done also a bit of refactor some local variables which were used
in multiple tests i.e., MAX_COMPACTION_LAG, MIN_CLEANABLE_DIRTY_RATIO
etc. but mostly it's 1:1 port (where I tried to merge it into existing
class as previously approved).

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
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20178 move logcleanerintegrationtest from core to storage (#21575).

**Test**
Test modified in LogCleanerIntegrationTest.scala covering the corrected behaviour.
