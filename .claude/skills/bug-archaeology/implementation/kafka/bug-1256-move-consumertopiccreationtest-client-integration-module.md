**Commit**: 20546930ae · **Ticket**: KAFKA-19042
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
This patch moves `ConsumerTopicCreationTest` to the
`client-integration-tests` and rewrite it as Java. The patch also streamlines the test flow. In the Scala version, there is a producer that produces messages, but
this is not the main purpose of the `ConsumerTopicCreationTest`. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
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
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19042 move consumertopiccreationtest to client-integration-tests module (#19283).

**Test**
Test modified in ConsumerTopicCreationTest.java covering the corrected behaviour.
