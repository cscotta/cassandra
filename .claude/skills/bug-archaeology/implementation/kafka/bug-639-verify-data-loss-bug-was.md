**Commit**: 57456b3c6c · **Ticket**: KAFKA-19479
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
Adds a new integration test to verify that we don't lose any data after
a MESSAGE_TOO_LARGE error on write. Reviewers: Matthias J. Sax <matthias@confluent.io>

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
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19479: add test to verify data-loss bug was fixed inside the producer (#20254).

**Test**
Test modified in AtLeastOnceDeliveryMessageLossIntegrationTest.java covering the corrected behaviour.
