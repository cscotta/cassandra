**Commit**: c0e77d1626 · **Ticket**: KAFKA-19914
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Migrate AlterPartitionManager from Scala to Java in the
`org.apache.kafka.server.partition` package, including the interface,
`DefaultAlterPartitionManager` implementation, and `AlterPartitionItem`
record. Also migrate `AlterPartitionManagerTest` and move
`MockAlterPartitionManager` from `TestUtils.scala` to
`org.apache.kafka.server.util`. Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import kafka.server.AlterPartitionManager;
import org.apache.kafka.server.partition.{AlterPartitionListener, AssignmentState, CommittedPartitionState, OngoingReassignmentState, PartitionListener, PartitionState, PendingExpandIsr, PendingPartitionChange, PendingShrinkIsr, SimpleAssignmentState}
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *

// After
    implementation project(':server').sourceSets.test.output
import org.apache.kafka.server.partition.AlterPartitionManager;
import org.apache.kafka.server.partition.{AlterPartitionListener, AlterPartitionManager, AssignmentState, CommittedPartitionState, OngoingReassignmentState, PartitionListener, PartitionState, PendingExpandIsr, PendingPartitionChange, PendingShrinkIsr, SimpleAssignmentState}
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19914: move alterpartitionmanager to server module (#21432).

**Test**
Test modified in LocalLeaderEndPointTest.scala covering the corrected behaviour.
