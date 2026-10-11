**Commit**: 24994ef231 · **Ticket**: KAFKA-20084
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
LogCleanerManagerTest has been moved to the storage module and rewritten
in Java. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

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
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20084 move logcleanermanagertest to storage module (#21343).

**Test**
Test modified in LogCleanerManagerTest.scala covering the corrected behaviour.
