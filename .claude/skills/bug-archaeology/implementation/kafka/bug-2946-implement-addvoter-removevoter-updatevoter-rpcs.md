**Commit**: 9ceed8f18f · **Ticket**: KAFKA-16535
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Implement the add voter, remove voter, and update voter RPCs for
KIP-853. This is just adding the RPC handling; the current
implementation in RaftManager just throws UnsupportedVersionException. Reviewers: Andrew Schofield <aschofield@confluent.io>, José Armando García Sancio <jsancio@apache.org>

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
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16535: implement addvoter, removevoter, updatevoter rpcs.

**Test**
Test modified in MockAdminClient.java covering the corrected behaviour.
