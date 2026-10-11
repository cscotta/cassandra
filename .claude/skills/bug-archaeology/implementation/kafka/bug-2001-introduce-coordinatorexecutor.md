**Commit**: a211ee99b5 · **Ticket**: KAFKA-17593
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
This patch introduces the `CoordinatorExecutor` construct into the `CoordinatorRuntime`. It allows scheduling asynchronous tasks from within a `CoordinatorShard` while respecting the runtime semantic. It will be used to asynchronously resolve regular expressions. The `GroupCoordinatorService` uses a default `ExecutorService` with a single thread to back it at the moment.

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
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [7/n] introduce coordinatorexecutor (#17823).

**Test**
Test modified in CoordinatorExecutorImplTest.java covering the corrected behaviour.
