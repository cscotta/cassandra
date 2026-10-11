**Commit**: 42604b46c2 · **Ticket**: KAFKA-19807
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
## What
Ticket: https://issues.apache.org/jira/browse/KAFKA-19807

Add integration test similar to `ShareGroupHeartbeatRequestTest` and
`ConsumerGroupHeartbeatRequestTest` for `StreamsGroupHeartbeat`

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

---------

Co-authored-by: Lucas Brutschy <lbrutschy@gmail.com>
Co-authored-by: Copilot <175728472+Copilot@users.noreply.github.com>

**Before/After**
```java
// Before
// (no removals)

// After
    processId: String = null,
        .setProcessId(processId)
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
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19807: add rpc-level integration tests for streamsgroupheartbeat [1/2] (#20757).

**Test**
Test modified in GroupCoordinatorBaseRequestTest.scala covering the corrected behaviour.
