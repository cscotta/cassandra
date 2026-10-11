**Commit**: dd0fd55316 · **Ticket**: KAFKA-18319
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Introduces interfaces for defining task assignors. Task assignors are pure functions, mapping the state of the group and a topology to a target assignment. We include a mock assignor, which we will be able to use when testing / benchmarking without the complexities of the sticky task assignor and the high-availability task assignor. We may remove the mock assignor in before the streams rebalance protocol goes GA.

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18319: add task assignor interfaces (#18270).

**Test**
Test modified in GroupSpecImplTest.java covering the corrected behaviour.
