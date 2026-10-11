**Commit**: ec32c8a376 · **Ticket**: KAFKA-18282
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
The StreamsGroupHeartbeat API is the new core API used by streams application to form a group. The API allows members to initialize a topology, advertise their state, and their owned tasks. The group coordinator uses it to assign/revoke tasks to/from members. This API is also used as a liveness check.

**Before/After**
```java
// Before
    READ_SHARE_GROUP_STATE_SUMMARY(ApiMessageType.READ_SHARE_GROUP_STATE_SUMMARY, true);

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
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18282: add streamsgroupheartbeat rpc definitions (#18227).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
