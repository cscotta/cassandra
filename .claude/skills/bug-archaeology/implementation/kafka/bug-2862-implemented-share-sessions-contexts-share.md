**Commit**: 8f6e0513df · **Ticket**: KAFKA-16747
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
About

KIP-932 introduces share sessions for share groups. This PR implements share sessions and contexts for incoming share fetch requests on broker. The changes include:

Defined CachedSharePartition class which are stored in share sessions. Defined ShareSessionKey, ShareSession classes.

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
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16747: implemented share sessions and contexts for share fetch requests (#16263).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
