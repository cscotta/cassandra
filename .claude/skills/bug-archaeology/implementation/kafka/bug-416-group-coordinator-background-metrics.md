**Commit**: 244e848e01 · **Ticket**: KAFKA-20211
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
Add the background-queue-time and background-processing-time histogram
metrics. Add the background-thread-idle-ratio-avg metric. Unlike the event
processor, we cannot measure idle time when using an ExecutorService. Instead, we measure busy time and invert it when reporting the metric.

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
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20211: add group coordinator background metrics (#21616).

**Test**
Test modified in CoordinatorBackgroundThreadPoolExecutorTest.java covering the corrected behaviour.
