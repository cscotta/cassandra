**Commit**: 87777846d6 · **Ticket**: KAFKA-20124
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR ports `LogCleanerLagIntegrationTest` from core module into
storage. I went through each line and ported the 1:1 way as much as
possible. Moreover I have ported `AbstractLogCleanerIntegrationTest`
with additional methods, which would be needed for next ports (i.e.,
`LogCleanerParameterizedIntegrationTest`) and other integration classes. Tests for `storage` module are passing:
```java
BUILD SUCCESSFUL in 10m 39s
88 actionable tasks: 21 executed, 67 up-to-date
```

Reviewers: Mickael Maison <mimaison@users.noreply.github.com>, Chia-Ping
 Tsai <chia7712@gmail.com>

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
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20124 move logcleanerlagintegrationtest to storage module (#21406).

**Test**
Test modified in LogCleanerLagIntegrationTest.scala covering the corrected behaviour.
