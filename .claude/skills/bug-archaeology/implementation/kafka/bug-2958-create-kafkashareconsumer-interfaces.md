**Commit**: c3a1bef429 · **Ticket**: KAFKA-16715
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR introduces ShareConsumer and KafkaShareConsumer. It is focused entirely on the minimal additions required to introduce the external programming interfaces. Reviewers: Apoorv Mittal <amittal@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

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
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16715: create kafkashareconsumer interfaces (#16134).

**Test**
Test modified in MockShareConsumerTest.java covering the corrected behaviour.
