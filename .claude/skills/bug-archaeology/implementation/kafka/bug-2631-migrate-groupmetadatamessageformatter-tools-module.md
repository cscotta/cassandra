**Commit**: 704476885f · **Ticket**: KAFKA-16666
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
we need to migate GroupMetadataMessageFormatter from scala code to java code,and make the message format is json pattern

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  @Deprecated
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
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16666 migrate groupmetadatamessageformatter` to tools module (#16748).

**Test**
Test modified in ConsoleConsumerOptionsTest.java covering the corrected behaviour.
