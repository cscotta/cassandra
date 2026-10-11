**Commit**: 8f82f14a48 · **Ticket**: KAFKA-16713
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR defines the initial set of RPCs for KIP-932. The RPCs for the admin client and state management are not in this PR. Reviewers: Apoorv Mittal <amittal@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

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
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16713: define initial set of rpcs for kip-932 (#16022).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
