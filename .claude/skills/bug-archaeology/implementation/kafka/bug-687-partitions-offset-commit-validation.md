**Commit**: 69dd4c8c58 · **Ticket**: KAFKA-19779
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This adds code to pass committed partitions into  ClassicGroup,
StreamsGroup and ConsumerGroup. These are not used in validation yet,
this is just preparation for later PRs. The validation implementation in each group can return a validator,
that will be used to validate each partition in the offset commit. We also add variations of streams groups tests for the tests in
OffsetMetadataManagerTest, which previously only tested the consumer
group case.

**Before/After**
```java
// Before
    void validateOffsetCommit(

    private StreamsGroup getOrMaybeCreatePersistedStreamsGroup(
    private Group validateOffsetCommit(
        group.validateOffsetCommit(
        return group;

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19779: add partitions to offset commit validation [1/n] (#20714).

**Test**
Test modified in OffsetMetadataManagerTest.java covering the corrected behaviour.
