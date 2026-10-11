**Commit**: b998189b00 · **Ticket**: KAFKA-18538
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The Streams membership manager is used client-side in the
background thread of the async consumer. For each member
/consumer, it is responsible for:
* keeping the member state,
* keeping assignments for the member,
* reconciling the assignments of the member -- for example
when tasks need to be revoked before other tasks are assigned
* requesting invocations of assignment and revocation callbacks
by the stream thread. The Streams membership manager is called by the background thread of
the async consumer, directly in its event loop and from the
 Streams group heartbeat request manager. The Streams membership
manager uses the Streams rebalance events processor to request
assignment/revocation...

**Before/After**
```java
// Before
     * completes (callbacks executed and heartbeat request to leave is sent out). This will be empty is the

// After
     * completes (callbacks executed and heartbeat request to leave is sent out). This will be empty if the
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
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18538: add streams membership manager (#18551).

**Test**
Test modified in StreamsMembershipManagerTest.java covering the corrected behaviour.
