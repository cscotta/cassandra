**Commit**: 2beadc8bd0 · **Ticket**: KAFKA-17695
**Subsystem**: test-common/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        TestUtils.retryOnExceptionWithTimeout(60_000, () -> {
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

// After
        TestUtils.retryOnExceptionWithTimeout(() -> {
import static java.lang.String.format;

```

**How to spot**
When reviewing test-common/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17695 cleanup org.apache.kafka.common.test.testutils (#17364).

**Test**
Test modified in KafkaClusterTestKit.java covering the corrected behaviour.
