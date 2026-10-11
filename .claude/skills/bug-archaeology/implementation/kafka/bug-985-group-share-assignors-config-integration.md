**Commit**: 844b0e651b · **Ticket**: KAFKA-19369
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
* Add `group.share.assignors` config to `GroupCoordinatorConfig`. * Send `rackId` in share group heartbeat request if it's not null. * Add integration test `testShareConsumerWithRackAwareAssignor`. Reviewers: Lan Ding <53332773+DL1231@users.noreply.github.com>, Andrew
 Schofield <aschofield@confluent.io>

---------

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
public class RackAwareAssignor implements ConsumerGroupPartitionAssignor {

// After
import org.apache.kafka.coordinator.group.api.assignor.ShareGroupPartitionAssignor;
public class RackAwareAssignor implements ConsumerGroupPartitionAssignor, ShareGroupPartitionAssignor {
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
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19369: add group.share.assignors config and integration test (#19900).

**Test**
Test modified in RackAwareAssignor.java covering the corrected behaviour.
