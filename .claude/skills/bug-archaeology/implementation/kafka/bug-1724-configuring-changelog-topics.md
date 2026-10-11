**Commit**: 2521aee7d4 · **Ticket**: KAFKA-18311
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
A simplified port of "ChangelogTopics" from the client-side to the group coordinator

Compared to the client-side version, the implementation uses immutable data structures, and returns the computed number of partitions instead of modifying mutable data structures and calling the admin client. Reviewers: Bruno Cadonna <cadonna@apache.org>

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18311: configuring changelog topics (2/n) (#18379).

**Test**
Test modified in ChangelogTopicsTest.java covering the corrected behaviour.
