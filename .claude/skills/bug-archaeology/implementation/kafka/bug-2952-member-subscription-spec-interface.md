**Commit**: 078dd9a311 · **Ticket**: KAFKA-16821
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch reworks the `PartitionAssignor` interface to use interfaces instead of POJOs. It mainly introduces the `MemberSubscriptionSpec` interface that represents a member subscription and changes the `GroupSpec` interfaces to expose the subscriptions and the assignments via different methods. The patch does not change the performance. before:
```
Benchmark                                     (memberCount)  (partitionsToMemberRatio)  (topicCount)  Mode  Cnt  Score   Error  Units
TargetAssignmentBuilderBenchmark.build                10000                         10           100  avgt    5  3.462 ± 0.687  ms/op
TargetAssignmentBuilderBenchmark.build                10000                     ...

**Before/After**
```java
// Before
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

// After
// (no additions)
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16821; member subscription spec interface (#16068).

**Test**
Test modified in AssignmentTestUtil.java covering the corrected behaviour.
