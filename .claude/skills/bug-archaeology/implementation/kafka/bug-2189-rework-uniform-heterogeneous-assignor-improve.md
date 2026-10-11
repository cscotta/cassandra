**Commit**: 017da21099 · **Ticket**: KAFKA-17710
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Rework the uniform heterogeneous assignor to improve performance, while
preserving the high level ideas and structure from the existing
implementation:
 * The assignor works in 3 stages: importing the previous assignment for
   stickiness, assigning unassigned partitions and iteratively
   reassigning partitions to improve balance. * Unassigned partitions are assigned to the subscribers with the least
   number of partitions. This maximizes balance within a single topic. * During the iterative rebalancing phase, partitions are reassigned to
   their previous owner if it improves balance (stickiness restoration).

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17710; rework uniform heterogeneous assignor to improve perf (#17385).

**Test**
Test modified in UniformHeterogeneousAssignmentBuilderTest.java covering the corrected behaviour.
