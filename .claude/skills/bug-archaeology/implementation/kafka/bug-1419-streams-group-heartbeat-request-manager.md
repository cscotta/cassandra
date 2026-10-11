**Commit**: d6b6952d48 · **Ticket**: KAFKA-18736
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This commit adds the Streams group heartbeat request manager
to the async consumer. The Streams group heartbeat request
manager is responsible to send heartbeat requests and to
process their responses. This commit implements:
- sending of full heartbeat request (independent of any state)
- processing successful response

Reviewers: Bill Bejeck <bill@confluent.io>, Lucas Brutschy <lbrutschy@confluent.io>

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
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18736: add streams group heartbeat request manager (1/n) (#18870).

**Test**
Test modified in StreamsGroupHeartbeatRequestManagerTest.java covering the corrected behaviour.
