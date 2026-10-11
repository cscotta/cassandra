**Commit**: adee6f0cc1 · **Ticket**: KAFKA-16527
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Implement request handling for the updated versions of the KRaft RPCs (Fetch, FetchSnapshot, Vote,
BeginQuorumEpoch and EndQuorumEpoch). This doesn't add support for KRaft replicas to send the new
version of the KRaft RPCs. That will be implemented in KAFKA-16529. All of the RPCs responses were extended to include the leader's endpoint for the listener of the
channel used in the request.

**Before/After**
```java
// Before
    FENCED_STATE_EPOCH(124, "The share coordinator rejected the request because the share-group state epoch did not match.", FencedStateEpochException::new);

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
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16527; implement request handling for updated kraft rpcs (#16235).

**Test**
Test modified in TestRaftRequestHandler.scala covering the corrected behaviour.
